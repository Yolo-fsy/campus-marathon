# -*- coding: utf-8 -*-
"""把合成的报名 CSV 转成 MySQL INSERT 语句，灌入业务库供实时看板演示。

用法：python import_csv_to_sql.py
输入：data/marathon_registrations.csv
输出：data/marathon_import.sql（INSERT IGNORE，学号唯一键防重复导入）
"""

import csv
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(ROOT, 'data', 'marathon_registrations.csv')
SQL_PATH = os.path.join(ROOT, 'data', 'marathon_import.sql')

FIELDS = ('name', 'student_id', 'phone', 'college', 'event_category',
          'status', 'pay_status', 'out_trade_no', 'pay_amount',
          'pay_time', 'remark', 'created_at')


def sql_value(raw):
    if raw is None or raw == '':
        return 'NULL'
    return "'" + str(raw).replace('\\', '\\\\').replace("'", "''") + "'"


def main():
    with open(CSV_PATH, 'r', encoding='utf-8-sig', newline='') as f:
        rows = list(csv.DictReader(f))

    stmts = ['-- 由 scripts/import_csv_to_sql.py 自动生成：合成报名数据灌库',
             'USE campus_marathon;']
    for r in rows:
        stmts.append(
            'INSERT IGNORE INTO marathon_runner '
            '(name, student_id, phone, college, event_category, status, pay_status, '
            'out_trade_no, pay_amount, pay_time, remark, created_at) VALUES ('
            + ', '.join(sql_value(r[k]) for k in FIELDS) + ');'
        )

    with open(SQL_PATH, 'w', encoding='utf-8') as f:
        f.write('\n'.join(stmts))
    print(f'生成 {len(rows)} 条 INSERT → {SQL_PATH}')


if __name__ == '__main__':
    main()
