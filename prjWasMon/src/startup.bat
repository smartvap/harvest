@echo off

rem 首先检查JAVA_HOME,未设置则设置环境变量
if not defined JAVA_HOME goto :set_env
rem 若已设置,则进行下一步操作
goto :clear_quot

:set_env
set /p JAVA_HOME=运行此程序要求安装JDK1.6,若已安装,请指定JAVA_HOME路径:
rem 若用户只输入回车,则环境变量无法定义,继续环境变量设置
if not defined JAVA_HOME goto :set_env

:clear_quot
rem 清除JAVA_HOME中的无效字符[引号]
set JAVA_HOME=%JAVA_HOME:"=%
rem 若用户输入了无效的路径,则继续环境变量设置
if not exist "%JAVA_HOME%"\bin\java.exe (
  echo 路径无效!
  goto :set_env
)

:check_version
rem 检查JVM版本,若版本正确则执行下一步
echo 检查JDK版本...
"%JAVA_HOME%"\bin\java.exe -version 2>&1 | findstr "java version" | findstr "1.6.0" 1>nul && goto :set_reg
echo JDK版本必须为JDK1.6
goto :set_env

:set_reg
rem 环境变量需要更新到注册表
rem 若注册表相关条目存在则与JAVA_HOME比较
echo 查询注册表环境变量配置...
reg query "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME 2>nul | findstr /i JAVA_HOME 1>nul 2>nul && goto :cmp_reg
rem 若条目不存在则添加条目
goto :add_reg

:cmp_reg
echo 取出注册表键值和JAVA_HOME比较
for /f "tokens=1,2,* " %%i in ('reg query "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME ^| findstr /i JAVA_HOME') do set reg_value=%%k
echo 若匹配,则运行程序
if "%reg_value%" == "%JAVA_HOME%" goto :start

:add_reg
rem 若注册表键值未添加或配置不正确,则强制添加/修改
echo 强制添加/覆盖注册表环境变量...
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME /t REG_SZ /d "%JAVA_HOME%" /f 1>nul 2>nul
echo 允许powershell运行本地未签名脚本...
powershell -command {Set-ExecutionPolicy RemoteSigned}
echo 广播消息[环境改变]...
powershell -file broadcast_environment.ps1 1>nul 2>nul

:start
echo 启动Derby数据库...
"%JAVA_HOME%"\bin\java.exe -cp .;..\libs\derby\derby.jar;..\libs\derby\derbyrun.jar;..\libs\derby\derbynet.jar;..\libs\derby\derbytools.jar -Dderby.system.home=../bin/derby org.apache.derby.drda.NetworkServerControl start -h localhost -p 1527
echo 运行PerfReader...
"%JAVA_HOME%"\bin\java.exe -cp .;..\bin;..\libs\com.ibm.jaxws.thinclient_7.0.0.jar;..\libs\com.ibm.ws.admin.client_7.0.0.jar;..\libs\com.ibm.ws.webservices.thinclient_7.0.0.jar;..\libs\j2ee.jar;..\libs\was_public.jar;..\libs\ibmjsseprovider2.jar;..\libs\fastjson-1.2.37.jar;..\libs\jide_demo.jar;..\libs\ini4j-0.5.4.jar;..\libs\commons-lang-2.6\commons-lang-2.6-javadoc.jar;..\libs\commons-lang-2.6\commons-lang-2.6-sources.jar;..\libs\commons-lang-2.6\commons-lang-2.6.jar PerfReader

:end
@echo on