import urllib.request
import json
req = urllib.request.Request('http://localhost:8080/api/bookings', data=json.dumps({'roomId':1, 'startDate':'2024-05-15', 'endDate':'2024-05-18', 'occupants':1}).encode('utf-8'), headers={'Content-Type': 'application/json', 'X-User-Id': '2'})
try:
    res = urllib.request.urlopen(req, timeout=120)
    print('CODE:', res.getcode())
except Exception as e:
    data = json.loads(e.read().decode('utf-8'))
    print('STATUS:', data.get('status'))
    print('ERROR:', data.get('error'))
    print('MESSAGE:', data.get('message'))
