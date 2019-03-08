#! /bin/bash
#####################################################
# 运行GOAi Web服务
#####################################################

exist_jar_home=$(cat /etc/profile|grep 'export JRE_HOME')
if [[ -z "${exist_jar_home}" ]]; then
    source /etc/profile
fi

if type java > /dev/null 2>&1; then
    echo -e "检测JDK环境正常"
else
    echo -e "JDK未安装，无法启动服务" && exit 1
fi

JAR_PATH="./application/web.jar"
GOAI_PORT=7758

COUNT=`ps -ef|grep -w java|grep -w "\-jar"|grep -w "${JAR_PATH}"|grep -v grep | wc -l`
if [ $COUNT -gt 0 ]; then
    echo "GOAi服务正在运行中，无需再次启动"
else
    nohup java -Xms64m -Xmx250m -jar ${JAR_PATH} --server.port=${GOAI_PORT} > /dev/null 2>&1 &

    echo "GOAi服务已开启，若需开启远程访问，请先确保开放对应端口及设置防火墙"
fi
