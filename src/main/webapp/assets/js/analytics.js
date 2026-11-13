(function () {
    'use strict';

    var ctx = window.AX_CTX || '';
    var charts = {};

    var C = {
        orange: '#ff5a1f',
        blue: '#1f4f8b',
        green: '#10b981',
        amber: '#f59e0b',
        red: '#dc2626',
        gray: '#94a3b8',
        orangeSoft: 'rgba(255,90,31,.15)'
    };

    Chart.defaults.font.family = '"PingFang SC", "Microsoft YaHei", sans-serif';
    Chart.defaults.color = '#4b5563';

    function $(id) {
        return document.getElementById(id);
    }

    function fmtPct(value) {
        return (value * 100).toFixed(1) + '%';
    }

    function fmtMoney(value) {
        return '¥' + Number(value).toFixed(2);
    }

    function destroyChart(id) {
        if (charts[id]) {
            charts[id].destroy();
            delete charts[id];
        }
    }

    function options(title, extra) {
        var base = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                title: { display: true, text: title },
                legend: { position: 'bottom' }
            }
        };
        if (extra && extra.scales) {
            base.scales = extra.scales;
        }
        if (extra && extra.indexAxis) {
            base.indexAxis = extra.indexAxis;
        }
        return base;
    }

    function render(data) {
        $('axTotal').textContent = data.total;
        $('axConfirmed').textContent = data.confirmed;
        $('axConfirmRate').textContent = fmtPct(data.confirmRate);
        $('axPayRate').textContent = fmtPct(data.payConversion);
        $('axMeta').textContent = '更新于 ' + data.generatedAt + ' · 每 30 秒自动刷新';
        $('axEmpty').hidden = data.total !== 0;

        var codes = Object.keys(data.events || {});
        var labels = codes.map(function (code) {
            return (data.events[code] && data.events[code].label) || code;
        });
        var counts = codes.map(function (code) {
            return data.eventCounts[code] || 0;
        });
        var capacities = codes.map(function (code) {
            return (data.events[code] && data.events[code].capacity) || 0;
        });

        destroyChart('eventShare');
        charts.eventShare = new Chart($('chartEventShare'), {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: counts,
                    backgroundColor: [C.orange, C.blue, C.green],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: options('报名项目占比')
        });

        destroyChart('eventCapacity');
        charts.eventCapacity = new Chart($('chartEventCapacity'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [
                    { label: '已报名', data: counts, backgroundColor: C.orange, borderRadius: 6 },
                    { label: '容量上限', data: capacities, backgroundColor: '#e5e7eb', borderRadius: 6 }
                ]
            },
            options: options('项目容量使用情况', { scales: { y: { beginAtZero: true } } })
        });

        destroyChart('status');
        charts.status = new Chart($('chartStatus'), {
            type: 'doughnut',
            data: {
                labels: ['待审核', '报名成功', '已驳回', '已取消'],
                datasets: [{
                    data: [data.pending, data.confirmed, data.rejected, data.cancelled],
                    backgroundColor: [C.amber, C.green, C.red, C.gray],
                    borderWidth: 2,
                    borderColor: '#fff'
                }]
            },
            options: options('报名状态分布')
        });

        destroyChart('pay');
        charts.pay = new Chart($('chartPay'), {
            type: 'bar',
            data: {
                labels: ['未缴费', '已缴费', '已退款'],
                datasets: [{
                    data: [
                        data.payCounts.UNPAID || 0,
                        data.payCounts.PAID || 0,
                        data.payCounts.REFUNDED || 0
                    ],
                    backgroundColor: [C.amber, C.green, C.red],
                    borderRadius: 6
                }]
            },
            options: options('支付状态', { indexAxis: 'y', scales: { x: { beginAtZero: true } } })
        });

        var dailyDates = (data.daily || []).map(function (row) { return row.date; });
        var dailyCounts = (data.daily || []).map(function (row) { return row.count; });

        destroyChart('daily');
        charts.daily = new Chart($('chartDaily'), {
            type: 'line',
            data: {
                labels: dailyDates,
                datasets: [{
                    label: '新增报名',
                    data: dailyCounts,
                    borderColor: C.orange,
                    backgroundColor: C.orangeSoft,
                    tension: 0.3,
                    fill: true,
                    pointRadius: 4
                }]
            },
            options: options('每日新增报名', { scales: { y: { beginAtZero: true, title: { display: true, text: '人数' } } } })
        });

        var top8 = (data.collegeTotals || []).slice(0, 8);

        destroyChart('college');
        charts.college = new Chart($('chartCollege'), {
            type: 'bar',
            data: {
                labels: top8.map(function (row) { return row.name; }),
                datasets: [{
                    label: '报名人数',
                    data: top8.map(function (row) { return row.count; }),
                    backgroundColor: C.blue,
                    borderRadius: 6
                }]
            },
            options: options('学院报名 TOP8', { indexAxis: 'y', scales: { x: { beginAtZero: true } } })
        });

        renderHeat(data, codes, labels);
        renderRevenue(data);
    }

    /**
     * 学院 × 项目热力矩阵：颜色深浅随报名人数变化。
     */
    function renderHeat(data, codes, labels) {
        var rows = data.crossMatrix || [];
        var max = 0;
        rows.forEach(function (row) {
            codes.forEach(function (code) {
                max = Math.max(max, row.counts[code] || 0);
            });
        });

        var html = '<table><thead><tr><th>学院</th><th>合计</th>';
        labels.forEach(function (label) {
            html += '<th>' + label + '</th>';
        });
        html += '</tr></thead><tbody>';

        rows.forEach(function (row) {
            var total = 0;
            codes.forEach(function (code) {
                total += row.counts[code] || 0;
            });
            html += '<tr><td>' + row.college + '</td><td class="ax-heat-total">' + total + '</td>';
            codes.forEach(function (code) {
                var value = row.counts[code] || 0;
                html += '<td><span class="ax-heat-cell" style="background:' + heatColor(value, max) + '">' + value + '</span></td>';
            });
            html += '</tr>';
        });
        html += '</tbody></table>';
        $('axHeat').innerHTML = html;
    }

    function heatColor(value, max) {
        if (!value) {
            return '#eef1f6';
        }
        var ratio = Math.min(value / Math.max(max, 1), 1);
        var r = Math.round(255 - 60 * ratio);
        var g = Math.round(180 - 128 * ratio);
        var b = Math.round(120 - 90 * ratio);
        return 'rgb(' + r + ',' + g + ',' + b + ')';
    }

    function renderRevenue(data) {
        var paid = data.payCounts.PAID || 0;
        var refunded = data.payCounts.REFUNDED || 0;
        $('axRevenuePaid').innerHTML =
            '<span class="ax-revenue-count">' + paid + ' 笔已支付</span>' + fmtMoney(data.revenuePaid);
        $('axRevenueRefunded').innerHTML =
            '<span class="ax-revenue-count">' + refunded + ' 笔已退款</span>' + fmtMoney(data.revenueRefunded);
        $('axRevenueNet').innerHTML = fmtMoney(data.revenueNet);
    }

    function load() {
        fetch(ctx + '/analytics/data', { headers: { 'Accept': 'application/json' } })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('HTTP ' + res.status);
                }
                return res.json();
            })
            .then(function (data) {
                if (data.error) {
                    throw new Error(data.error);
                }
                render(data);
            })
            .catch(function (err) {
                $('axMeta').textContent = '加载失败：' + err.message;
            });
    }

    $('axRefresh').addEventListener('click', load);
    load();
    setInterval(load, 30000);
})();
