HIVE UDF
========

## Credits
Modifed from https://github.com/petrabarus/HiveUDFs

## Compiling
This project uses Maven, compile it with:

    mvn package

## Functions
### GeoIP2
*GeoIP2* uses MaxMinds GeoIP2 Database to retrieve informations from an IP Address. Any V2 database can be used, [paid](https://www.maxmind.com/en/geoip2-databases) or [lite](http://dev.maxmind.com/geoip/geoip2/geolite2/).

Usage:

    ADD JAR hive-udf.jar;
    ADD FILE GeoIP2-Country.mmdb;
    CREATE TEMPORARY FUNCTION geoip as 'com.spuul.hive.GeoIP2';
    SELECT geoip('8.8.8.8','COUNTRY_NAME','./GeoIP2-Country.mmdb');

####_FUNC_(*String* ip, *String* dataType, *String* databasePath)
##### dataType
Allows you to retrive a specific information from the database. The wanted information needs to be available in the used database. You can't retrieve an *city* information from a *country* database.
- COUNTRY_CODE : [Country](https://www.maxmind.com/en/geoip2-country-database), [City](https://www.maxmind.com/en/geoip2-city)
- COUNTRY_NAME : [Country](https://www.maxmind.com/en/geoip2-country-database), [City](https://www.maxmind.com/en/geoip2-city)
- SUBDIVISION_NAME : [City](https://www.maxmind.com/en/geoip2-city)
- SUBDIVISION_CODE : [City](https://www.maxmind.com/en/geoip2-city)
- CITY : [City](https://www.maxmind.com/en/geoip2-city)
- POSTAL_CODE : [City](https://www.maxmind.com/en/geoip2-city)
- LONGITUDE : [City](https://www.maxmind.com/en/geoip2-city)
- LATITUDE : [City](https://www.maxmind.com/en/geoip2-city)
- ASN : [ISP](https://www.maxmind.com/en/geoip2-isp-database)
- ASN_ORG : [ISP](https://www.maxmind.com/en/geoip2-isp-database)
- ISP : [ISP](https://www.maxmind.com/en/geoip2-isp-database)
- ORG: [ISP](https://www.maxmind.com/en/geoip2-isp-database)
- IS_ANONYMOUS : [Anonymous IP](https://www.maxmind.com/en/geoip2-anonymous-ip-database)
- IS_ANONYMOUS_VPN : [Anonymous IP](https://www.maxmind.com/en/geoip2-anonymous-ip-database)
- IS_ISP : [Anonymous IP](https://www.maxmind.com/en/geoip2-anonymous-ip-database)
- IS_PUBLIC_PROXY : [Anonymous IP](https://www.maxmind.com/en/geoip2-anonymous-ip-database)
- IS_TOR_EXIT_NODE : [Anonymous IP](https://www.maxmind.com/en/geoip2-anonymous-ip-database)
- DOMAIN : [Domain Name](https://www.maxmind.com/en/geoip2-domain-name-database)
- CONNECTION : [Connection Type](https://www.maxmind.com/en/geoip2-connection-type-database)