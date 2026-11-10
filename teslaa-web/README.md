# teslaa-web

The hosted web page of WebAA: the front end you open in the Tesla (or any) browser. It connects to
the WebAA app running on the phone and shows the Android Auto screen, plays audio and sends
touch input back. See the main [README](../README.md) for how it fits together.

## Run locally (development)

```bash
npm i
npm start
```

`npm start` serves `src/` over HTTPS with `http-server --ssl`, which needs a TLS certificate and key
named `cert.pem` and `key.pem` in this folder. Use your own certificate for the domain you serve
the page from (for example a Let's Encrypt one). Both files are gitignored, never commit them.

To test against a phone, point the hostnames at the right machines in your hosts file, e.g.:

```text
127.0.0.1       <your web domain>
<phone ip>      <your API domain>
```

## Run with nginx / Docker

`nginx.conf` serves the static files from `/usr/share/nginx/html` on port 80, and `run.sh` starts
nginx in the foreground, e.g. as a container entrypoint. Copy the contents of `src/` into that folder
and put HTTPS in front of it (reverse proxy or load balancer); the page has to be served over HTTPS.
