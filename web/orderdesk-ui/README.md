# orderdesk-ui (Angular 18)

The Angular CLI scaffold is not committed (it is ~100 generated files). Create it once, then the
committed `src/app` files drop in:

```bash
cd web
npx -y @angular/cli@18 new orderdesk-ui --standalone --routing=false --style=css --skip-git --skip-tests=false
# answer "no" to SSR
cd orderdesk-ui
npm i @ngrx/signals@18 @angular/cdk@18
# copy the committed src/app/* over the generated ones (git checkout -- src/app if you cloned first)
npm start -- --proxy-config proxy.conf.json      # http://localhost:4200, proxies /api -> :8080
```
