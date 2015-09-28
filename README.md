# spring-xd-phd3-sandbox
Demo sandbox with with an Ambari vm and PHD 3 vm cluster. Integrates Spring XD hdfs and GPDB. Also integrates with GemFire, Redis, etc.

## Installation

Installation instructions are here:

http://blog.tzolov.net/2015/06/leverage-vagrant-and-ambari-blueprint.html

Installation has been verified verbatim on macbook pro.  No issues noted. However, sleep tends to bounce HBase -- maintenance mode is therefore recommended.

The only caveat here is the XD config: you'll need to bind to namenode host adapter on eth1 (10.211.55.101), not the NAT adapter (Ambari only shows the NAT adapter on eth0). 

## Configure XD as follows:

1) Add the following to
/usr/local/Cellar/springxd/1.2.0.RELEASE/libexec/xd/config/hadoop.properties:
```
fs.default.name=hdfs://10.211.55.101:8020 
```
2) Add or uncomment the following to
/usr/local/Cellar/springxd/1.2.0.RELEASE/libexec/xd/config/servers.yml:

```
---
# Hadoop properties
spring:
  hadoop:
  fsUri: hdfs://10.211.55.101:8020
```

3) Spin up the XD service with 

<path-to-spring-xd>/xd-singlenode --hadoopDistro phd30

4) Spawn the XD shell with

<path-to-spring-xd>/xd-shell --hadoopDistro phd30

5) Configure the hdfs namenode in the shell

xd:> hadoop config fs --namenode hdfs://10.211.55.101:8020
