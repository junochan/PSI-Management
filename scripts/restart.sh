#!/bin/bash
. /etc/profile &>/dev/null
. /etc/rc.d/init.d/functions
. /server/scripts/get_project_mem.sh &>/dev/null
java=/usr/local/jdk17/bin/java
Dir=$(cd `dirname $0`;pwd)
project=$(basename $Dir)
backend_port=28586

get_mem $project &>/dev/null
[ -d $(dirname $Dir)/pid ]||mkdir $(dirname $Dir)/pid
pidDir=$(dirname $Dir)/pid

if [ "$Mem" == "" ]; then
    Mem="2048"
else
  expr 1 + $Mem &>/dev/null
  if [ $? -ne 0 ];then
    echo "参数错误...";exit 1
  fi
fi
# 判断jar包是否存在,项目名为当前所在目录名
if [[ ! -f $Dir/${project}.jar ]]; then
    echo "没有找到${project}.jar,无法启动"
    exit 1
fi
status_service(){
ps aux|grep $Dir/${project}.jar|grep -v grep
}

start_service(){
JAVA_OPTS="-Xms${Mem}m -Xmx${Mem}m -XX:MaxGCPauseMillis=200 -Duser.timezone=GMT+08 -Dfile.encoding=utf-8"
pid=$(ps aux|grep $Dir/${project}.jar|grep -v grep|awk '{print $2}')
if [[ -f $pidDir/${project}.pid && ! -z $pid ]]; then
  echo "service allready started."
  exit 0
fi
nohup ${java} ${JAVA_OPTS} -server -jar $Dir/${project}.jar --server.port="${backend_port}" > $Dir/run.log 2>&1 &
echo ${!} > $pidDir/${project}.pid
sleep 2
Pid=$(ps -ef|grep ${project}.jar|grep -v grep|awk '{print $2}')
if [[ -f $pidDir/${project}.pid && ! -z $Pid ]]; then
  action 'service start success.' /bin/true
else
  action 'service start fail.' /bin/false
fi
}

stop_service(){
if [[ ! -f $pidDir/${project}.pid ]]; then
   action "service allready stoped." /bin/true
else
   kill -9 `cat $pidDir/${project}.pid` &>/dev/null
   rm -f $pidDir/${project}.pid
   sleep 3
   action "service stoped success." /bin/true
fi
}
main(){ 
case $1 in 

start) 
        start_service;;
stop)
        stop_service;;
status)
        status_service;;
restart)
        stop_service && start_service;;
*)
        stop_service && start_service;;
esac
}
main $1
