# AV REST API
This is the Anti-Virus REST API

## How to test it
EICAR1='X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H'
curl -k -d "${EICAR1}*" -u pam:access https://localhost:8080/rest/api/1.0/av-scan/files/eicar.com
{"id":"4bb733d3-d527-4bec-8c38-cb043f0ee44b","name":"eicar.com","sha512":"cc805d5fab1fd71a4ab352a9c533e65fb2d5b885518f4e565e68847223b8e6b85cb48f3afad842726d99239c9e36505c64b0dc9a061d9e507d833277ada336ab","size":68,"status":"accepted","queuedAt":"2023-11-30T06:55:21.366+00:00","source":"pam@127.0.0.1"}

In the container logs you can see:
2023-11-30 07:55:21.374  INFO 75770 --- [nio-8080-exec-7] i.g.k.a.s.i.AvScanningService            : Scanning is queued for FileInfo { id=4bb733d3-d527-4bec-8c38-cb043f0ee44b, name=eicar.com, hash=cc805d5fab1fd71a4ab352a9c533e65fb2d5b885518f4e565e68847223b8e6b85cb48f3afad842726d99239c9e36505c64b0dc9a061d9e507d833277ada336ab, size=68, queuedAt=Thu Nov 30 07:55:21 CET 2023, analyzedAt=null, status=accepted, localPath=/home/user/Documents/workspace-spring-tool-suite-4-4.17.1.RELEASE/av-api/./4bb733d3-d527-4bec-8c38-cb043f0ee44b.com, source=pam@127.0.0.1 } from UsernamePasswordAuthenticationToken [Principal=pam, Credentials=[PROTECTED], Authenticated=true, Details=null, Granted Authorities=[ROLE_USER, ROLE_PAM]]
2023-11-30 07:55:46.013  INFO 75770 --- [pool-2-thread-3] i.g.k.a.s.i.ScanStrategyCmdline          : File analysis finished: FileInfo { id=4bb733d3-d527-4bec-8c38-cb043f0ee44b, name=eicar.com, hash=cc805d5fab1fd71a4ab352a9c533e65fb2d5b885518f4e565e68847223b8e6b85cb48f3afad842726d99239c9e36505c64b0dc9a061d9e507d833277ada336ab, size=68, queuedAt=Thu Nov 30 07:55:21 CET 2023, analyzedAt=Thu Nov 30 07:55:46 CET 2023, status=infected, localPath=/home/user/Documents/workspace-spring-tool-suite-4-4.17.1.RELEASE/av-api/./4bb733d3-d527-4bec-8c38-cb043f0ee44b.com, source=pam@127.0.0.1 }

## How to use container
podman run --cap-add cap_sys_admin -d -p 8080:8080 docker.io/digitalforensic/avrest:latest




