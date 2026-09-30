if not exist impl-app\target\config (
  cp -r impl-app\config impl-app\target\config
)

cd impl-app\target
java -Dsun.misc.URLClassPath.disableJarChecking=true -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6001 -jar impl-app-1.0.0-SNAPSHOT.jar
cd ..\..