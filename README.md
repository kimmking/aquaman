# Aquaman Gateway

Aquaman Gateway is a simple,flexible,high-performance gateway for MicroServices using Vert.X and Asynchronous Httpclient.


## Getting Started

1. fetch aquaman gateway codes from github and build jar:
```bash
git clone https://github.com/kimmking/aquaman
cd aquaman
gradle build
```

2. run gateway server at port 80, proxy all requests to backend server http://localhost:8088:
```bash
java -jar build/libs/aquaman-1.0-SNAPSHOT-all.jar -DproxyPort=8000 -DproxyServer=http://localhost:8088
```

## Features

Now:
- \[✔\]HTTP request inbound, via Vert.X
- \[✔\]Proxy HTTP request to backend endpoints, via Asynchronous Httpclient
- \[✔\]Configure proxyPort&proxyServer


Plan:
- \[x\]Configure specified API proxy
- \[x\]Filter to enhance inbound&outbound
- \[x\]Router to dynamic proxy to specified backend endpoints
- \[x\]FlowControl to limit api access
- \[x\]HTTPS/SSL support
- \[x\]WebSocket support
- \[x\]Authentication support
- \[x\]Blacklist support


## Benchmark


