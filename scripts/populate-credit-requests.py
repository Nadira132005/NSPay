#!/usr/bin/env python3
"""Upsert three complete credit submissions through the running local application's API."""
import argparse
import json
import os
from urllib import request, error
import uuid


def pdf(text):
    content = ('BT /F1 12 Tf 25 160 Td (' + text.replace('(', '').replace(')', '') + ') Tj ET').encode()
    objects = [b'<< /Type /Catalog /Pages 2 0 R >>', b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
               b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 600 220] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
               b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
               b'<< /Length ' + str(len(content)).encode() + b' >>\nstream\n' + content + b'\nendstream']
    result = b'%PDF-1.4\n'
    offsets = []
    for index, obj in enumerate(objects, 1):
        offsets.append(len(result))
        result += str(index).encode() + b' 0 obj\n' + obj + b'\nendobj\n'
    start = len(result)
    result += b'xref\n0 6\n0000000000 65535 f \n'
    result += b''.join(f'{offset:010d} 00000 n \n'.encode() for offset in offsets)
    return result + b'trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n' + str(start).encode() + b'\n%%EOF'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', default='http://127.0.0.1:8082')
    parser.add_argument('--username', default='bob')
    parser.add_argument('--password', default=os.environ.get('NSPAY_LOGIN_PASSWORD', 'DemoPay123!'))
    args = parser.parse_args()
    token = None

    def api(path, body=None, multipart=None):
        headers = {}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        data = None
        if multipart:
            application, kind, filename, contents = multipart
            boundary = uuid.uuid4().hex
            data = b''
            for key, value in [('applicationId', str(application)), ('kind', kind)]:
                data += f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode()
            data += f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: application/pdf\r\n\r\n'.encode()
            data += contents + f'\r\n--{boundary}--\r\n'.encode()
            headers['Content-Type'] = 'multipart/form-data; boundary=' + boundary
        elif body is not None:
            headers['Content-Type'] = 'application/json'
            data = json.dumps(body).encode()
        req = request.Request(args.base_url.rstrip('/') + path, data=data, headers=headers)
        with request.urlopen(req, timeout=40) as response:
            return json.load(response)

    try:
        token = api('/api/login/', {'name': args.username, 'password': args.password})['token']
        overview = api('/api/finance/overview')
        if not overview['creditEligible'] or overview['user']['role'] != 'User':
            raise ValueError('Use an active, verified regular customer. Existing identity and status are preserved.')
        name = overview['user']['firstName'] + ' ' + overview['user']['lastName']
        plans = [('Professional development', 2400, 1800, 12),
                 ('Apartment renovation', 6500, 2300, 24),
                 ('Electric vehicle purchase', 14500, 3200, 36)]
        for purpose, amount, income, months in plans:
            overview = api('/api/finance/overview')
            application = next((c for c in overview['credits'] if c['purpose'] == purpose), None)
            if application is None:
                application = api('/api/finance/credit', {'purpose': purpose, 'amount': amount,
                                                         'monthlyIncome': income, 'termMonths': months})
            if application['status'] != 'DRAFT':
                print(f'{purpose}: {application["status"]}, existing submission preserved')
                continue
            if any(application[k] != v for k, v in [('amount', amount), ('monthlyIncome', income), ('termMonths', months)]):
                raise ValueError('An existing draft has different financial details; it was preserved.')
            for kind, filename, text in [
                ('IDENTITY', 'identity-record.pdf', f'Sample identity supporting record - {name}'),
                ('INCOME', 'income-statement.pdf', f'Sample income statement - {name} - Monthly income EUR {income}')]:
                if not any(d.get('applicationId') == application['id'] and d['kind'] == kind for d in overview['documents']):
                    api('/api/finance/documents', multipart=(application['id'], kind, filename, pdf(text)))
            result = api('/api/finance/credit/' + str(application['id']) + '/submit', {})
            print(f'{purpose}: {result["status"]}, complete with identity and income documents')
        return 0
    except (error.HTTPError, error.URLError, ValueError, KeyError) as exc:
        print('Population stopped: ' + (f'HTTP {exc.code}' if isinstance(exc, error.HTTPError) else str(exc)))
        return 1


if __name__ == '__main__':
    raise SystemExit(main())
