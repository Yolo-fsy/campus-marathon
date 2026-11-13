# -*- coding: utf-8 -*-
"""
校园马拉松报名数据生成 + 分析 + 交互式报告生成器。
纯 Python 标准库，不依赖 pandas / matplotlib / 第三方图表库。
用法：python generate_marathon_data.py
输出：
  1. data/marathon_registrations.csv  合成的报名数据（200+ 条）
  2. report/marathon_analysis.html     交互式分析报告（Chart.js）
"""

import csv
import html
import os
import random
import datetime
from collections import Counter, defaultdict

_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(_ROOT, 'data')
REPORT_DIR = os.path.join(_ROOT, 'report')

EVENTS = {
    'HALF': {'label': '半程马拉松', 'capacity': 120, 'distance': '21.0975 km'},
    'MINI': {'label': '迷你跑', 'capacity': 200, 'distance': '5 km'},
    'FUN':  {'label': '欢乐跑', 'capacity': 300, 'distance': '3 km'},
}

COLLEGES = [
    ('计算机科学与技术学院', 28),
    ('机械工程学院', 22),
    ('信息与通信工程学院', 20),
    ('经济管理学院', 18),
    ('数学与统计学院', 15),
    ('物理学院', 12),
    ('材料科学与工程学院', 14),
    ('外国语学院', 11),
    ('新闻与传播学院', 10),
    ('艺术设计学院', 9),
    ('化学与化工学院', 13),
    ('生命科学学院', 11),
]

SURNAMES = list('林陈苏王李张刘杨赵周何沈顾许吴郑孙马朱胡郭')
NAMES = ['一舟', '晓棠', '子昂', '清和', '知远', '星遥', '青禾', '南风', '明澈', '致远',
         '书颜', '彦成', '知夏', '景行', '予安', '知秋', '墨尘', '听澜', '望舒', '沐阳',
         '云舒', '鹤鸣', '知行', '若水', '怀瑾', '向晚', '归南', '怀南', '听雨', '望舒']


def seed(seed_val=20261110):
    random.seed(seed_val)


def student_id_prefix(year):
    return f'{year:04d}'


def gen_student_id(used_ids):
    while True:
        year = random.choices([2022, 2023, 2024, 2025], weights=[15, 35, 35, 15])[0]
        prefix = student_id_prefix(year)
        body = random.randint(100000, 999999)
        sid = f'{prefix}{body}'
        if sid not in used_ids:
            used_ids.add(sid)
            return sid


def gen_phone():
    prefixes = ['138', '139', '150', '159', '186', '188', '137', '135']
    return random.choice(prefixes) + f'{random.randint(10000000, 99999999)}'


def gen_name():
    return random.choice(SURNAMES) + random.choice(NAMES)


def gen_created_at():
    """报名时间：集中在开赛前 2 周，最后 3 天最密集。"""
    base = datetime.datetime(2025, 8, 25, 0, 0, 0)
    days = random.choices(range(0, 15), weights=[3, 4, 5, 6, 8, 10, 12, 14, 16, 18, 20, 15, 10, 8, 5])[0]
    hour = random.randint(7, 22)
    minute = random.randint(0, 59)
    return base + datetime.timedelta(days=days, hours=hour - 7, minutes=minute)


def gen_status_and_pay(created_at):
    """根据报名时间模拟审核和支付状态。"""
    days_passed = (datetime.datetime(2025, 9, 11) - created_at).days

    if days_passed < 2:
        # 刚报名，大部分还没审核
        return 'PENDING', 'UNPAID', None, None

    status_rand = random.random()
    pay_rand = random.random()

    if status_rand < 0.65:
        status = 'CONFIRMED'
        if pay_rand < 0.75:
            pay = 'PAID'
            pay_time = created_at + datetime.timedelta(minutes=random.randint(5, 30))
            out_trade = f'MR{created_at.strftime("%Y%m%d%H%M%S")}{random.randint(1000, 9999)}'
            return status, pay, out_trade, pay_time
        elif pay_rand < 0.82:
            pay = 'REFUNDED'
            pay_time = created_at + datetime.timedelta(minutes=random.randint(10, 45))
            out_trade = f'MR{created_at.strftime("%Y%m%d%H%M%S")}{random.randint(1000, 9999)}'
            return status, pay, out_trade, pay_time
        else:
            return status, 'UNPAID', None, None
    elif status_rand < 0.80:
        return 'PENDING', 'UNPAID', None, None
    elif status_rand < 0.92:
        return 'REJECTED', 'UNPAID', None, None
    else:
        return 'CANCELLED', 'REFUNDED', None, None


def gen_registrations(total=200):
    """按学院权重生成，确保各项目容量接近真实分布。"""
    seed()
    used_ids = set()
    records = []

    # 先填满容量的 80-90%
    plan = [
        ('HALF', int(EVENTS['HALF']['capacity'] * random.uniform(0.75, 0.85))),
        ('MINI', int(EVENTS['MINI']['capacity'] * random.uniform(0.70, 0.82))),
        ('FUN',  int(EVENTS['FUN']['capacity'] * random.uniform(0.55, 0.70))),
    ]

    for event_code, count in plan:
        for _ in range(count):
            # 按学院权重选
            weights = [c[1] for c in COLLEGES]
            total_w = sum(weights)
            r = random.uniform(0, total_w)
            cum = 0
            college = COLLEGES[0][0]
            for name, w in COLLEGES:
                cum += w
                if r <= cum:
                    college = name
                    break

            sid = gen_student_id(used_ids)
            phone = gen_phone()
            name = gen_name()
            created = gen_created_at()
            status, pay, order, pay_time = gen_status_and_pay(created)
            remark = None
            if random.random() < 0.12:
                remarks = [
                    '第一次参加半马，希望有配速员带跑',
                    '需要寄存大件行李', '对花生过敏，请医护组留意',
                    '建议设饮水站间距 5km', '希望增设急救站',
                    '报名后能否更换参赛项目？', '领取物资时间希望延长',
                    '希望有官方摄影师', '是否可以带亲友随行？',
                ]
                remark = random.choice(remarks)

            records.append({
                'student_id': sid, 'name': name, 'phone': phone,
                'college': college, 'event_category': event_code,
                'status': status, 'pay_status': pay,
                'pay_amount': 30.00, 'out_trade_no': order,
                'pay_time': pay_time.isoformat(sep=' ') if pay_time else None,
                'remark': remark, 'created_at': created.isoformat(sep=' '),
            })

    random.shuffle(records)
    return records


def write_csv(records, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    fieldnames = ['student_id', 'name', 'phone', 'college', 'event_category',
                  'status', 'pay_status', 'pay_amount', 'out_trade_no',
                  'pay_time', 'remark', 'created_at']
    with open(path, 'w', newline='', encoding='utf-8-sig') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for r in records:
            writer.writerow(r)


def compute_metrics(records):
    """聚合统计，供图表使用。"""
    total = len(records)
    by_event = Counter(r['event_category'] for r in records)
    by_status = Counter(r['status'] for r in records)
    by_pay = Counter(r['pay_status'] for r in records)
    by_college = Counter(r['college'] for r in records)

    # 确认率 = CONFIRMED / (CONFIRMED + PENDING + REJECTED + CANCELLED)
    confirmed = by_status.get('CONFIRMED', 0)
    pending = by_status.get('PENDING', 0)
    rejected = by_status.get('REJECTED', 0)
    cancelled = by_status.get('CANCELLED', 0)
    confirm_rate = confirmed / max(1, confirmed + pending + rejected + cancelled)

    # 支付转化率
    paid = by_pay.get('PAID', 0)
    refunded = by_pay.get('REFUNDED', 0)
    pay_conversion = paid / max(1, confirmed)

    # 每日报名数
    daily = Counter()
    for r in records:
        day = r['created_at'][:10]
        daily[day] += 1

    # 学院 × 项目交叉
    cross = defaultdict(lambda: Counter())
    for r in records:
        cross[r['college']][r['event_category']] += 1

    # 已缴 + 已退 + 未缴的金额
    revenue_paid = paid * 30.00
    revenue_refunded = refunded * 30.00
    revenue_net = revenue_paid - revenue_refunded

    return {
        'total': total, 'confirmed': confirmed, 'pending': pending,
        'rejected': rejected, 'cancelled': cancelled,
        'confirm_rate': confirm_rate, 'pay_conversion': pay_conversion,
        'paid': paid, 'refunded': refunded,
        'revenue_paid': revenue_paid, 'revenue_refunded': revenue_refunded,
        'revenue_net': revenue_net,
        'by_event': dict(by_event), 'by_status': dict(by_status),
        'by_pay': dict(by_pay), 'by_college': dict(by_college.most_common(8)),
        'daily': dict(sorted(daily.items())),
        'cross': {c: dict(v) for c, v in cross.items()},
        'capacity': {k: v['capacity'] for k, v in EVENTS.items()},
    }


def e(s):
    """HTML 转义。"""
    return html.escape(str(s))


def build_chart_labels(keys):
    return '[' + ', '.join('"' + e(k) + '"' for k in keys) + ']'


def build_chart_numbers(values):
    return '[' + ', '.join(str(v) for v in values) + ']'


def build_report(metrics):
    """生成包含 Chart.js 的交互式 HTML 报告。"""
    os.makedirs(REPORT_DIR, exist_ok=True)
    path = os.path.join(REPORT_DIR, 'marathon_analysis.html')

    event_labels = list(EVENTS.keys())
    event_names = [EVENTS[k]['label'] for k in event_labels]
    event_nums = [metrics['by_event'].get(k, 0) for k in event_labels]
    event_cap = [metrics['capacity'].get(k, 0) for k in event_labels]
    event_usage = [round(n / max(1, c) * 100, 1) for n, c in zip(event_nums, event_cap)]

    status_labels = ['PENDING 待审核', 'CONFIRMED 报名成功',
                     'REJECTED 已驳回', 'CANCELLED 已取消']
    status_keys = ['PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED']
    status_nums = [metrics['by_status'].get(k, 0) for k in status_keys]

    pay_labels = ['UNPAID 未缴费', 'PAID 已缴费', 'REFUNDED 已退款']
    pay_keys = ['UNPAID', 'PAID', 'REFUNDED']
    pay_nums = [metrics['by_pay'].get(k, 0) for k in pay_keys]

    college_data = list(metrics['by_college'].items())
    college_names = [c[0] for c in college_data]
    college_nums = [c[1] for c in college_data]

    daily_keys = list(metrics['daily'].keys())
    daily_vals = [metrics['daily'][k] for k in daily_keys]

    # 学院 × 项目热力数据
    event_codes = list(EVENTS.keys())
    event_labels2 = [EVENTS[k]['label'] for k in event_codes]
    all_colleges = sorted(metrics['cross'].keys(),
                          key=lambda c: sum(metrics['cross'][c].values()), reverse=True)

    event_college_data_js = []
    for col in all_colleges:
        row = [metrics['cross'][col].get(k, 0) for k in event_codes]
        event_college_data_js.append('[' + ', '.join(str(v) for v in row) + ']')

    # 预计算交叉矩阵的 HTML 行（避免 f-string 嵌套 f-string）
    max_cap = max(metrics['capacity'].values())
    cross_rows_html = []
    for col in all_colleges:
        row_total = sum(metrics['cross'][col].values())
        cells_html = []
        for k in event_codes:
            v = metrics['cross'][col].get(k, 0)
            bg = heat_color(v, max_cap)
            cells_html.append(
                '<td><span class=heat-cell style=background:{}>{}</span></td>'.format(bg, v)
            )
        cross_rows_html.append(
            '<tr><td>{}</td><td style="font-weight:700">{}</td>{}</tr>'.format(
                e(col), row_total, ''.join(cells_html)
            )
        )
    cross_rows_html_str = ''.join(cross_rows_html)

    # 预计算表头（避免 f-string 嵌套）
    table_headers_str = ''.join('<th>{}</th>'.format(e(label)) for label in event_labels2)

    total = metrics['total']
    confirmed = metrics['confirmed']
    paid = metrics['paid']
    refunded = metrics['refunded']
    pay_rate = metrics['pay_conversion']
    conf_rate = metrics['confirm_rate']

    html_doc = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>校园马拉松报名数据可视化报告</title>
<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
<style>
  * {{ box-sizing: border-box; margin: 0; padding: 0; }}
  body {{ background: #f4f6fa; font-family: -apple-system, "PingFang SC", "Microsoft YaHei", sans-serif; color: #1b1f24; line-height: 1.6; }}
  .container {{ max-width: 1100px; margin: 0 auto; padding: 32px 20px 60px; }}
  header {{ padding: 28px 32px; background: linear-gradient(135deg, #ff5a1f, #ef4f13); border-radius: 14px; color: #fff; margin-bottom: 24px; }}
  header h1 {{ font-size: 24px; letter-spacing: 1px; }}
  header p {{ margin-top: 6px; opacity: 0.85; font-size: 13px; }}
  .kpi-grid {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 24px; }}
  .kpi {{ background: #fff; border-radius: 12px; padding: 18px 20px; box-shadow: 0 2px 10px rgba(20,35,60,.08); }}
  .kpi .num {{ font-size: 28px; font-weight: 700; color: #ff5a1f; }}
  .kpi .label {{ font-size: 12.5px; color: #6b7280; margin-top: 4px; letter-spacing: 0.5px; }}
  .kpi .num.secondary {{ color: #1f4f8b; }}
  .section {{ background: #fff; border-radius: 12px; padding: 22px 24px; margin-bottom: 18px; box-shadow: 0 2px 10px rgba(20,35,60,.08); }}
  .section h2 {{ font-size: 15px; letter-spacing: 2px; color: #1f4f8b; padding-bottom: 10px; border-bottom: 1px solid #e5e7eb; margin-bottom: 18px; }}
  .section h2::before {{ content: ""; display: inline-block; width: 3px; height: 12px; background: #ff5a1f; margin-right: 8px; vertical-align: -1px; }}
  .chart-row {{ display: grid; grid-template-columns: 1fr 1fr; gap: 18px; }}
  .chart-row.full {{ grid-template-columns: 1fr; }}
  .chart-box {{ position: relative; height: 280px; }}
  @media (max-width: 760px) {{
    .kpi-grid {{ grid-template-columns: repeat(2, 1fr); }}
    .chart-row {{ grid-template-columns: 1fr; }}
  }}
  table {{ width: 100%; border-collapse: collapse; font-size: 13px; }}
  th, td {{ padding: 8px 12px; text-align: left; border-bottom: 1px solid #f1f3f6; }}
  th {{ background: #f9fafb; color: #4b5563; font-weight: 600; }}
  tr:hover {{ background: #f9fafb; }}
  .heat-cell {{ font-weight: 600; color: #fff; text-align: center; padding: 6px 8px; border-radius: 4px; font-size: 12.5px; }}
</style>
</head>
<body>
<div class="container">

<header>
  <h1>🏃‍♂️ 校园马拉松 · 报名数据可视化报告</h1>
  <p>基于 2025 年 8–9 月赛事报名数据 · 共 {total} 条记录 · 生成时间 2025-09-09 10:00</p>
</header>

<section class="kpi-grid">
  <div class="kpi"><div class="num">{total}</div><div class="label">总报名人数</div></div>
  <div class="kpi"><div class="num secondary">{confirmed}</div><div class="label">报名成功（CONFIRMED）</div></div>
  <div class="kpi"><div class="num">{conf_rate:.1%}</div><div class="label">确认率（审核通过）</div></div>
  <div class="kpi"><div class="num">{pay_rate:.1%}</div><div class="label">支付转化率（成功/已确认）</div></div>
</section>

<section class="section">
  <h2>参赛项目分布</h2>
  <div class="chart-row">
    <div class="chart-box"><canvas id="eventPie"></canvas></div>
    <div class="chart-box"><canvas id="eventBar"></canvas></div>
  </div>
</section>

<section class="section">
  <h2>报名状态与支付漏斗</h2>
  <div class="chart-row">
    <div class="chart-box"><canvas id="statusDoughnut"></canvas></div>
    <div class="chart-box"><canvas id="payFunnel"></canvas></div>
  </div>
</section>

<section class="section">
  <h2>每日报名趋势</h2>
  <div class="chart-row full">
    <div class="chart-box" style="height:300px"><canvas id="dailyLine"></canvas></div>
  </div>
</section>

<section class="section">
  <h2>学院报名 TOP8</h2>
  <div class="chart-row full">
    <div class="chart-box" style="height:320px"><canvas id="collegeBar"></canvas></div>
  </div>
</section>

<section class="section">
  <h2>学院 × 项目交叉矩阵</h2>
  <table>
    <thead><tr><th>学院</th><th>合计</th>{table_headers_str}</tr></thead>
    <tbody>
      {cross_rows_html_str}
    </tbody>
  </table>
</section>

<section class="section">
  <h2>财务汇总</h2>
  <table>
    <tbody>
      <tr><td>已收报名费（{paid} 笔 × ¥30）</td><td style="text-align:right;font-weight:700;color:#059669">¥{metrics['revenue_paid']:.2f}</td></tr>
      <tr><td>已退款（{refunded} 笔 × ¥30）</td><td style="text-align:right;font-weight:700;color:#dc2626">¥{metrics['revenue_refunded']:.2f}</td></tr>
      <tr style="border-top:2px solid #e5e7eb"><td>净收入</td><td style="text-align:right;font-weight:800;color:#1f4f8b;font-size:15px">¥{metrics['revenue_net']:.2f}</td></tr>
    </tbody>
  </table>
</section>

</div>

<script>
Chart.defaults.font.family = '"PingFang SC", "Microsoft YaHei", sans-serif';
Chart.defaults.color = '#4b5563';

// 项目分布饼图
new Chart(document.getElementById('eventPie'), {{
  type: 'doughnut',
  data: {{ labels: {build_chart_labels(event_names)}, datasets: [{{ data: {build_chart_numbers(event_nums)}, backgroundColor: ['#ff5a1f','#1f4f8b','#10b981'], borderWidth: 2, borderColor: '#fff' }}] }},
  options: {{ responsive: true, plugins: {{ title: {{ display: true, text: '报名项目占比' }}, legend: {{ position: 'bottom' }} }} }}
}});

// 项目容量使用率柱状图
new Chart(document.getElementById('eventBar'), {{
  type: 'bar',
  data: {{
    labels: {build_chart_labels(event_names)},
    datasets: [
      {{ label: '已报名', data: {build_chart_numbers(event_nums)}, backgroundColor: '#ff5a1f', borderRadius: 6 }},
      {{ label: '容量上限', data: {build_chart_numbers(event_cap)}, backgroundColor: '#e5e7eb', borderRadius: 6 }}
    ]
  }},
  options: {{ responsive: true, plugins: {{ title: {{ display: true, text: '容量使用率' }} }}, scales: {{ y: {{ beginAtZero: true }}, x: {{ stacked: true }}, y: {{ stacked: true }} }} }}
}});

// 报名状态环形图
new Chart(document.getElementById('statusDoughnut'), {{
  type: 'doughnut',
  data: {{ labels: {build_chart_labels(status_labels)}, datasets: [{{ data: {build_chart_numbers(status_nums)}, backgroundColor: ['#f59e0b','#10b981','#dc2626','#6b7280'], borderWidth: 2, borderColor: '#fff' }}] }},
  options: {{ responsive: true, plugins: {{ title: {{ display: true, text: '报名状态分布' }}, legend: {{ position: 'bottom' }} }} }}
}});

// 支付漏斗
new Chart(document.getElementById('payFunnel'), {{
  type: 'bar',
  data: {{ labels: {build_chart_labels(pay_labels)}, datasets: [{{ data: {build_chart_numbers(pay_nums)}, backgroundColor: ['#f59e0b','#10b981','#dc2626'], borderRadius: 6 }}] }},
  options: {{ responsive: true, indexAxis: 'y', plugins: {{ title: {{ display: true, text: '支付状态' }} }} }}
}});

// 每日报名趋势
new Chart(document.getElementById('dailyLine'), {{
  type: 'line',
  data: {{ labels: {build_chart_labels(daily_keys)}, datasets: [{{ label: '新增报名', data: {build_chart_numbers(daily_vals)}, borderColor: '#ff5a1f', backgroundColor: 'rgba(255,90,31,.15)', tension: 0.3, fill: true, pointRadius: 4 }}] }},
  options: {{ responsive: true, plugins: {{ title: {{ display: true, text: '每日新增报名' }} }}, scales: {{ y: {{ beginAtZero: true, title: {{ display: true, text: '人数' }} }} }} }}
}});

// 学院 TOP8
new Chart(document.getElementById('collegeBar'), {{
  type: 'bar',
  data: {{ labels: {build_chart_labels(college_names)}, datasets: [{{ label: '报名人数', data: {build_chart_numbers(college_nums)}, backgroundColor: '#1f4f8b', borderRadius: 6 }}] }},
  options: {{ responsive: true, indexAxis: 'y', plugins: {{ title: {{ display: true, text: '学院报名 TOP8' }} }}, scales: {{ x: {{ beginAtZero: true }} }} }}
}});
</script>
</body>
</html>
"""

    with open(path, 'w', encoding='utf-8') as f:
        f.write(html_doc)
    return path


def heat_color(value, max_value):
    """热力图单元格颜色，绿色越深代表数值越大。"""
    if value == 0:
        return '#e5e7eb'
    ratio = min(value / max(max_value, 1) * 0.85 + 0.15, 1.0)
    # 红橙渐变
    r = int(220 - 130 * (1 - ratio))
    g = int(38 + 80 * ratio)
    b = int(30)
    return f'rgb({r},{g},{b})'


def main():
    records = gen_registrations(total=215)
    csv_path = os.path.join(OUT_DIR, 'marathon_registrations.csv')
    write_csv(records, csv_path)
    metrics = compute_metrics(records)
    report_path = build_report(metrics)
    print(f'CSV → {csv_path}')
    print(f'HTML → {report_path}')
    print(f'总记录 {metrics["total"]} 条 · 成功 {metrics["confirmed"]} · 确认率 {metrics["confirm_rate"]:.1%} · 支付转化率 {metrics["pay_conversion"]:.1%}')


if __name__ == '__main__':
    main()
