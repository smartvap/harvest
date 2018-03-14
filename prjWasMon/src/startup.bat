@echo off

rem ���ȼ��JAVA_HOME,δ���������û�������
if not defined JAVA_HOME goto :set_env
rem ��������,�������һ������
goto :clear_quot

:set_env
set /p JAVA_HOME=���д˳���Ҫ��װJDK1.6,���Ѱ�װ,��ָ��JAVA_HOME·��:
rem ���û�ֻ����س�,�򻷾������޷�����,����������������
if not defined JAVA_HOME goto :set_env

:clear_quot
rem ���JAVA_HOME�е���Ч�ַ�[����]
set JAVA_HOME=%JAVA_HOME:"=%
rem ���û���������Ч��·��,�����������������
if not exist "%JAVA_HOME%"\bin\java.exe (
  echo ·����Ч!
  goto :set_env
)

:check_version
rem ���JVM�汾,���汾��ȷ��ִ����һ��
echo ���JDK�汾...
"%JAVA_HOME%"\bin\java.exe -version 2>&1 | findstr "java version" | findstr "1.6.0" 1>nul && goto :set_reg
echo JDK�汾����ΪJDK1.6
goto :set_env

:set_reg
rem ����������Ҫ���µ�ע���
rem ��ע��������Ŀ��������JAVA_HOME�Ƚ�
echo ��ѯע�������������...
reg query "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME 2>nul | findstr /i JAVA_HOME 1>nul 2>nul && goto :cmp_reg
rem ����Ŀ�������������Ŀ
goto :add_reg

:cmp_reg
echo ȡ��ע����ֵ��JAVA_HOME�Ƚ�
for /f "tokens=1,2,* " %%i in ('reg query "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME ^| findstr /i JAVA_HOME') do set reg_value=%%k
echo ��ƥ��,�����г���
if "%reg_value%" == "%JAVA_HOME%" goto :start

:add_reg
rem ��ע����ֵδ��ӻ����ò���ȷ,��ǿ�����/�޸�
echo ǿ�����/����ע���������...
reg add "HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v JAVA_HOME /t REG_SZ /d "%JAVA_HOME%" /f 1>nul 2>nul
echo ����powershell���б���δǩ���ű�...
powershell -command {Set-ExecutionPolicy RemoteSigned}
echo �㲥��Ϣ[�����ı�]...
powershell -file broadcast_environment.ps1 1>nul 2>nul

:start
echo ����Derby���ݿ�...
"%JAVA_HOME%"\bin\java.exe -cp .;..\libs\derby\derby.jar;..\libs\derby\derbyrun.jar;..\libs\derby\derbynet.jar;..\libs\derby\derbytools.jar -Dderby.system.home=../bin/derby org.apache.derby.drda.NetworkServerControl start -h localhost -p 1527
echo ����PerfReader...
"%JAVA_HOME%"\bin\java.exe -cp .;..\bin;..\libs\com.ibm.jaxws.thinclient_7.0.0.jar;..\libs\com.ibm.ws.admin.client_7.0.0.jar;..\libs\com.ibm.ws.webservices.thinclient_7.0.0.jar;..\libs\j2ee.jar;..\libs\was_public.jar;..\libs\ibmjsseprovider2.jar;..\libs\fastjson-1.2.37.jar;..\libs\jide_demo.jar;..\libs\ini4j-0.5.4.jar;..\libs\commons-lang-2.6\commons-lang-2.6-javadoc.jar;..\libs\commons-lang-2.6\commons-lang-2.6-sources.jar;..\libs\commons-lang-2.6\commons-lang-2.6.jar PerfReader

:end
@echo on