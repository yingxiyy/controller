1.编译controller
mvn clean install -Dmaven.test.skip=true  -Dcheckstyle.skip -DskipTests -Dmaven.javadoc.skip=true
2. 将包controller/assemble/target/distribution-1.0.0-SNAPSHOT.tar.gz上传到目标服务器的某个目录，比如/var/tmp/
3. 创建软件目录，并解压软件至此。比如：
mkdir /home/deploy
tar -xvzf distribution-1.0.0-SNAPSHOT.tar.gz -C /home/deploy --no-same-owner
4. 配置/home/deploy/env.config
5. 执行脚本安装：
./install.sh
Note: 安装脚本会安装解压目录下的所有软件，所以如果那些app是不想安装的，删除即可。

6.安装telemetry
在telemetry的工程里面，打包distribution，得到包Telemetry-Distribution-v1.1.0.6-bin.tar.gz，和上面一样解压到某个目录，比如:/home/deploy, 然后安装。
tar -xvzf Telemetry-Distribution-v1.1.0.6-bin.tar.gz  -C /home/deploy/
./install.sh    

Note： 因为目前telemetry和controller是不同的工程，所以暂时分开打包的。 等稍后telemetry相关app稳定了，到时再修改。