# Ubuntu 部署说明

这份文档保留当前项目真正需要的部署步骤，删掉了不常用的背景说明。平时更新版本时，重点看“日常更新部署”这一节即可。

适用环境：

- 本地开发机：Windows
- 本地 JDK：`D:\jdk-17.0.17`
- 本地 Maven：`D:\maven\apache-maven-3.6.3`
- 服务器系统：Ubuntu 24.04
- 前端部署方式：静态文件 + Nginx
- 后端部署方式：Spring Boot `jar` + `systemd`
- 外部访问端口：`8088`

## 一、当前部署路径

服务器统一使用以下目录：

```text
/opt/helpdesk/backend
/opt/helpdesk/frontend
/opt/helpdesk/uploads
/opt/helpdesk/uploads/operation-guides
```

其中：

- `/opt/helpdesk/backend/helpdesk-backend-1.0.0.jar`：后端可执行包
- `/opt/helpdesk/frontend`：前端静态文件目录
- `/opt/helpdesk/uploads`：普通上传附件目录
- `/opt/helpdesk/uploads/operation-guides`：操作说明附件目录

当前服务：

- 后端服务名：`helpdesk`
- 前端服务名：`nginx`
- 后端端口：`8081`
- 对外访问端口：`8088`

当前默认数据库配置：

- 数据库主机：`127.0.0.1`
- 数据库端口：`3306`
- 数据库名：`helpdesk_dev`
- 数据库用户：`root`
- 数据库密码：`12345678`

当前上传目录配置在：

- `backend/src/main/resources/application.yml`

默认值是：

```yaml
app:
  upload-dir: ../uploads
```

因为后端在 `/opt/helpdesk/backend` 启动，所以它实际对应的是：

```text
/opt/helpdesk/uploads
```

## 二、首次部署

如果服务器已经跑起来过，并且 `nginx`、`mysql`、`helpdesk.service` 都配好了，可以直接跳到“日常更新部署”。
mk
### 1. 安装基础环境

在 Ubuntu 服务器执行：

```bash
apt update
apt install -y openjdk-17-jdk nginx mysql-server
systemctl enable nginx mysql
systemctl start nginx mysql
```

### 2. 创建目录

```bash
mkdir -p /opt/helpdesk/backend
mkdir -p /opt/helpdesk/frontend
mkdir -p /opt/helpdesk/uploads
mkdir -p /opt/helpdesk/uploads/operation-guides
```

### 3. 初始化数据库

进入 MySQL：

```bash
sudo mysql -u root
```

执行：

```sql
SET GLOBAL validate_password.policy = LOW;
SET GLOBAL validate_password.length = 8;

CREATE DATABASE IF NOT EXISTS helpdesk_dev DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER USER 'root'@'localhost' IDENTIFIED WITH caching_sha2_password BY '12345678';
CREATE USER IF NOT EXISTS 'root'@'127.0.0.1' IDENTIFIED WITH caching_sha2_password BY '12345678';
ALTER USER 'root'@'127.0.0.1' IDENTIFIED WITH caching_sha2_password BY '12345678';

GRANT ALL PRIVILEGES ON *.* TO 'root'@'localhost' WITH GRANT OPTION;
GRANT ALL PRIVILEGES ON *.* TO 'root'@'127.0.0.1' WITH GRANT OPTION;
FLUSH PRIVILEGES;
EXIT;
```

### 4. 注册后端服务

在 Ubuntu 服务器执行：

```bash
cat > /etc/systemd/system/helpdesk.service <<'EOF'
[Unit]
Description=Helpdesk Backend
After=network.target mysql.service

[Service]
User=root
WorkingDirectory=/opt/helpdesk/backend
ExecStart=/usr/bin/java -jar /opt/helpdesk/backend/helpdesk-backend-1.0.0.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

然后执行：

```bash
systemctl daemon-reload
systemctl enable helpdesk
```

### 5. 配置 Nginx

在 Ubuntu 服务器执行：
```bash
cat > /etc/nginx/sites-available/helpdesk <<'EOF'
server {
    listen 8088;
    server_name 116.128.189.228 _;

    root /opt/helpdesk/frontend;
    index index.html;
    client_max_body_size 50m;

    location /assets/ {
        alias /opt/helpdesk/frontend/assets/;
    }

    location = /runtime-config.js {
        alias /opt/helpdesk/frontend/runtime-config.js;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /uploads/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
EOF
```

启用配置：

```bash
rm -f /etc/nginx/sites-enabled/default
ln -sf /etc/nginx/sites-available/helpdesk /etc/nginx/sites-enabled/helpdesk
nginx -t
systemctl reload nginx
```

## 三、本地打包

### 1. 打包前端

在 Windows 本地执行：

```cmd
cd /d c:\Users\zengyiming\Desktop\gd\frontend
npm run build
```

产物目录：

```text
c:\Users\zengyiming\Desktop\gd\frontend\dist
```

### 2. 打包后端

在 Windows 本地执行：

```cmd
cd /d c:\Users\zengyiming\Desktop\gd\backend
set JAVA_HOME=D:\jdk-17.0.17
set M2_HOME=D:\maven\apache-maven-3.6.3
set PATH=D:\jdk-17.0.17\bin;D:\maven\apache-maven-3.6.3\bin;%PATH%
D:\maven\apache-maven-3.6.3\bin\mvn.cmd -gs settings.xml -s settings.xml -DskipTests clean package
```

注意：

- 后端这里要用 `clean package`
- 上传到服务器的必须是 `backend\target\helpdesk-backend-1.0.0.jar`
- 这个 `jar` 要能直接 `java -jar` 启动

## 四、日常更新部署

这部分是最常用的。以后你改完前后端，基本就按这套流程走。

### 1. 停后端服务

在 Ubuntu 服务器执行：

```bash
systemctl stop helpdesk
systemctl reset-failed helpdesk
```

说明：

- 更新前端静态文件时一般不用停 `nginx`
- 只要停后端，上传完后再启动就行

### 2. 上传后端 `jar`

在 Windows 本地执行：

```cmd
cd /d c:\Users\zengyiming\Desktop\gd\backend\target
scp helpdesk-backend-1.0.0.jar root@116.128.189.228:/opt/helpdesk/backend/
```

### 3. 上传前端文件

在 Windows 本地执行：

```cmd
cd /d c:\Users\zengyiming\Desktop\gd\frontend\dist
scp index.html root@116.128.189.228:/opt/helpdesk/frontend/
scp runtime-config.js root@116.128.189.228:/opt/helpdesk/frontend/
scp -r assets root@116.128.189.228:/opt/helpdesk/frontend/
```

如果本地没有 `runtime-config.js`，可在服务器手动创建：

```bash
cat > /opt/helpdesk/frontend/runtime-config.js <<'EOF'
window.__HELPDESK_RUNTIME_CONFIG__ = {}
EOF
```

### 4. 上传操作说明附件

“操作说明”菜单会按当前语言直接下载下面两个文件，所以服务器上需要提前放好：

```text
/opt/helpdesk/uploads/operation-guides/操作说明-中文版.docx
/opt/helpdesk/uploads/operation-guides/Operation-Guide-English.docx
```

如果附件有变更，在 Windows 本地执行：

```cmd
cd /d c:\Users\zengyiming\Desktop\gd\uploads\operation-guides
scp "操作说明-中文版.docx" root@116.128.189.228:/opt/helpdesk/uploads/operation-guides/
scp Operation-Guide-English.docx root@116.128.189.228:/opt/helpdesk/uploads/operation-guides/
```

### 5. 修正前端文件权限

每次覆盖上传前端静态文件后，建议固定执行一次：

```bash
chmod 755 /opt
chmod 755 /opt/helpdesk
find /opt/helpdesk/frontend -type d -exec chmod 755 {} \;
find /opt/helpdesk/frontend -type f -exec chmod 644 {} \;
chown -R root:root /opt/helpdesk/frontend
```

### 6. 启动后端并重载前端

在 Ubuntu 服务器执行：

```bash
systemctl start helpdesk
systemctl status helpdesk --no-pager
journalctl -u helpdesk -n 50 --no-pager

nginx -t
systemctl reload nginx
systemctl status nginx --no-pager
```

## 五、部署后验证

### 1. 测首页

```bash
curl http://127.0.0.1:8088/
```

### 2. 测登录接口

```bash
curl -i -X POST http://127.0.0.1:8088/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"123456"}'
```

### 3. 测静态资源

先查真实文件名：

```bash
ls /opt/helpdesk/frontend/assets | grep '^index-.*\.js$'
```

再用查到的文件名测试，例如：

```bash
curl -I http://127.0.0.1:8088/assets/index-DQb-EXNk.js
```

注意：

- 每次前端重新打包后，`index-xxxx.js` 文件名都会变化
- 不要一直拿旧文件名测试

### 4. 测操作说明附件

```bash
curl -I http://127.0.0.1:8088/uploads/operation-guides/Operation-Guide-English.docx
```

### 5. 看服务状态

```bash
systemctl status helpdesk --no-pager
systemctl status nginx --no-pager
```

## 六、特殊情况处理

### 1. 前端静态资源报 `403 Forbidden`

现象：

- 浏览器里 `assets/index-xxxx.js` 返回 `403`
- `curl -I http://127.0.0.1:8088/assets/...` 也返回 `403`

原因：

- 这不是跨域问题
- 这通常是 `nginx` 没权限读取 `/opt/helpdesk/frontend` 下的静态文件

处理：

```bash
chmod 755 /opt
chmod 755 /opt/helpdesk
find /opt/helpdesk/frontend -type d -exec chmod 755 {} \;
find /opt/helpdesk/frontend -type f -exec chmod 644 {} \;
chown -R root:root /opt/helpdesk/frontend
nginx -t
systemctl reload nginx
```

### 2. 后端启动报 `no main manifest attribute`

现象：

```text
no main manifest attribute, in /opt/helpdesk/backend/helpdesk-backend-1.0.0.jar
```

原因：

- 上传的不是可执行 Spring Boot `jar`
- 通常是本地没有用正确方式重新打包

处理：

在 Windows 本地重新打包：

```cmd
cd /d c:\Users\zengyiming\Desktop\gd\backend
set JAVA_HOME=D:\jdk-17.0.17
set M2_HOME=D:\maven\apache-maven-3.6.3
set PATH=D:\jdk-17.0.17\bin;D:\maven\apache-maven-3.6.3\bin;%PATH%
D:\maven\apache-maven-3.6.3\bin\mvn.cmd -gs settings.xml -s settings.xml -DskipTests clean package
```

然后重新上传 `backend\target\helpdesk-backend-1.0.0.jar` 并执行：

```bash
systemctl stop helpdesk
systemctl reset-failed helpdesk
systemctl start helpdesk
journalctl -u helpdesk -n 50 --no-pager
```

### 3. 操作说明点击下载报 `404`

原因：

- 服务器没有这个目录，或者文件没上传

处理：

```bash
mkdir -p /opt/helpdesk/uploads/operation-guides
ls -l /opt/helpdesk/uploads/operation-guides
```

确保这两个文件存在：

```text
/opt/helpdesk/uploads/operation-guides/操作说明-中文版.docx
/opt/helpdesk/uploads/operation-guides/Operation-Guide-English.docx
```

### 4. 登录报错或接口不通

优先检查：

```bash
systemctl status helpdesk --no-pager
systemctl status mysql --no-pager
journalctl -u helpdesk -n 100 --no-pager
mysql -h 127.0.0.1 -u root -p -e "SHOW DATABASES LIKE 'helpdesk_dev';"
```

常见原因：

- MySQL 没启动
- 后端服务没启动成功
- 数据库账号密码没按当前项目配置设置

### 5. 后端服务一直重启

先停掉自动重启，避免刷屏：

```bash
systemctl stop helpdesk
systemctl reset-failed helpdesk
```

然后看日志：

```bash
journalctl -u helpdesk -n 100 --no-pager
```

### 6. 更新后前端还是旧页面

处理顺序：

```bash
nginx -t
systemctl reload nginx
```

然后浏览器里清缓存，或执行强刷：

- Windows：`Ctrl + F5`

如果还是不对，重新确认你上传的是最新的 `frontend/dist` 内容。

## 七、常用命令

后端：

```bash
systemctl stop helpdesk
systemctl start helpdesk
systemctl restart helpdesk
systemctl status helpdesk --no-pager
journalctl -u helpdesk -n 100 --no-pager
```

前端：

```bash
nginx -t
systemctl reload nginx
systemctl restart nginx
systemctl status nginx --no-pager
```

数据库：

```bash
systemctl status mysql --no-pager
mysql -h 127.0.0.1 -u root -p
```
