/**
 * GeoIP2.java.
 *
 * Copyright (C) 2015 Daniel Muller - Spuul,
 *
 * Copyright (C) 2013 Petra Barus,
 * https://github.com/petrabarus/HiveUDFs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.spuul.hive;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AnonymousIpResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.ConnectionTypeResponse;
import com.maxmind.geoip2.model.ConnectionTypeResponse.ConnectionType;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.model.DomainResponse;
import com.maxmind.geoip2.model.IspResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Continent;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Postal;
import com.maxmind.geoip2.record.Subdivision;

import org.apache.hadoop.hive.ql.exec.Description;
import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.udf.UDFType;
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorConverters;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

/**
 * This is a UDF to look a property of an IP address using MaxMind GeoIP2
 * library.
 *
 * The function will need three arguments. <ol> <li>IP Address in string
 * format.</li> <li>IP attribute (e.g. COUNTRY, CITY, REGION, etc)</li>
 * <li>Database file name.</li> </ol>
 *
 * This is a derived version from https://github.com/petrabarus/HiveUDFs.
 * (Please let me know if I need to modify the license)
 *
 * @author Daniel Muller <daniel@spuul.com>
 * @see https://github.com/petrabarus/HiveUDFs
 */
@UDFType(deterministic = true)
@Description(
  name = "geoip2",
value = "_FUNC_(ip,attribute,database) - looks a property for an IP address from"
+ "a library loaded\n"
+ "The GeoIP2 database comes separated. To load the GeoIP2 use ADD FILE.\n"
+ "Usage:\n"
+ " > _FUNC_(\"8.8.8.8\", \"COUNTRY_CODE\", \"./GeoIP2-Country.mmdb\")")
public class GeoIP2 extends GenericUDF {

        private ObjectInspectorConverters.Converter[] converters;

        /**
         * Initialize this UDF.
         *
         * This will be called once and only once per GenericUDF instance.
         *
         * @param arguments The ObjectInspector for the arguments
         * @throws UDFArgumentException Thrown when arguments have wrong types,
         * wrong length, etc.
         * @return The ObjectInspector for the return value
         */
        @Override
        public ObjectInspector initialize(ObjectInspector[] arguments) throws UDFArgumentException {
                if (arguments.length != 3) {
                        throw new UDFArgumentLengthException("_FUNC_ accepts 3 arguments. " + arguments.length
                                + " found.");
                }

                converters = new ObjectInspectorConverters.Converter[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                        converters[i] = ObjectInspectorConverters.getConverter(arguments[i],
                                PrimitiveObjectInspectorFactory.writableStringObjectInspector);
                }
                return PrimitiveObjectInspectorFactory.writableStringObjectInspector;
        }

        /**
         * Evaluate the UDF with the arguments.
         *
         * @param arguments The arguments as DeferedObject, use
         * DeferedObject.get() to get the actual argument Object. The Objects
         * can be inspected by the ObjectInspectors passed in the initialize
         * call.
         * @return The return value.
         */
        @Override
        public Object evaluate(GenericUDF.DeferredObject[] arguments) throws HiveException {

                assert (arguments.length == 3);
                String ip = ((Text) converters[0].convert(arguments[0].get())).toString();
                String attributeName = ((Text) converters[1].convert(arguments[1].get())).toString();
                String databaseName = ((Text) converters[2].convert(arguments[2].get())).toString();

                //Just in case there are more than one database filename attached.
                //We will just assume that two file with same filename are identical.
                File database = new File(databaseName);

                String retVal = "";

                try {
                        // This creates the DatabaseReader object, which should be reused across
                        // lookups.
                        DatabaseReader reader = new DatabaseReader.Builder(database).build();
                        String databaseType = reader.getMetadata().getDatabaseType();
                        InetAddress ipAddress = InetAddress.getByName(ip);

                        switch (databaseType) {
                                case "GeoIP2-Country":
                                        retVal = getVal(attributeName, reader.country(ipAddress));
                                        break;
                                case "GeoIP2-City":
                                        retVal = getVal(attributeName, reader.city(ipAddress));
                                        break;
                                case "GeoIP2-Anonymous-IP":
                                        retVal = getVal(attributeName, reader.anonymousIp(ipAddress));
                                        break;
                                case "GeoIP2-Connection-Type":
                                        retVal = getVal(attributeName, reader.connectionType(ipAddress));
                                        break;
                                case "GeoIP2-Domain":
                                        retVal = getVal(attributeName, reader.domain(ipAddress));
                                        break;
                                case "GeoIP2-ISP":
                                        retVal = getVal(attributeName, reader.isp(ipAddress));
                                        break;
                                default:
                                        throw new UnsupportedOperationException("Unknown database type " + databaseType);
                        }
                        return new Text(retVal);
                }
                catch(Exception e) {
                        throw new UnsupportedOperationException(e.getMessage());
                }
        }

        public static String getVal(String dataType, CountryResponse response) throws IOException, GeoIp2Exception {
                if (dataType.equals("COUNTRY_CODE") || dataType.equals("COUNTRY_NAME")) {
                        Country country = response.getCountry();
                        if (dataType.equals("COUNTRY_CODE")) {
                                return country.getIsoCode();
                        }
                        else {
                                return country.getName();
                        }
                }
                else {
                        throw new UnsupportedOperationException("Unable get " + dataType + " for Database Type " + response.getClass().getSimpleName());
                }
        }

        public static String getVal(String dataType, CityResponse response) throws IOException, GeoIp2Exception {
                if (dataType.equals("COUNTRY_CODE")
                    || dataType.equals("COUNTRY_NAME")
                    || dataType.equals("SUBDIVISION_NAME")
                    || dataType.equals("SUBDIVISION_CODE")
                    || dataType.equals("CITY")
                    || dataType.equals("POSTAL_CODE")
                    || dataType.equals("LONGITUDE")
                    || dataType.equals("LATITUDE")
                ) {
                        if (dataType.equals("COUNTRY_CODE") || dataType.equals("COUNTRY_NAME")) {
                                Country country = response.getCountry();
                                if (dataType.equals("COUNTRY_CODE")) {
                                        return country.getIsoCode();
                                }
                                else {
                                        return country.getName();
                                }
                        }
                        if (dataType.equals("SUBDIVISION_CODE") || dataType.equals("SUBDIVISION_NAME")) {
                                Subdivision subdivision = response.getMostSpecificSubdivision();
                                if (dataType.equals("SUBDIVISION_CODE")) {
                                        return subdivision.getIsoCode();
                                }
                                else {
                                        return subdivision.getName();
                                }
                        }
                        if (dataType.equals("CITY")) {
                                City city = response.getCity();
                                return city.getName();
                        }
                        if (dataType.equals("POSTAL_CODE")) {
                                Postal postal = response.getPostal();
                                return postal.getCode();
                        }
                        if (dataType.equals("LONGITUDE") || dataType.equals("LATITUDE")) {
                                Location location = response.getLocation();
                                if (dataType.equals("LONGITUDE")) {
                                        return location.getLongitude().toString();
                                }
                                else {
                                        return location.getLatitude().toString();
                                }
                        }
                        return "";
                }
                else {
                        throw new UnsupportedOperationException("Unable get " + dataType + " for Database Type " + response.getClass().getSimpleName());
                }
        }

        public static String getVal(String dataType, IspResponse response) throws IOException, GeoIp2Exception {
                String retVal = "";
                switch (dataType) {
                        case "ASN":
                                retVal = response.getAutonomousSystemNumber().toString();
                                break;
                        case "ASN_ORG":
                                retVal = response.getAutonomousSystemOrganization();
                                break;
                        case "ISP":
                                retVal = response.getIsp();
                                break;
                        case "ORG":
                                retVal = response.getOrganization();
                                break;
                        default:
                                throw new UnsupportedOperationException("Unable get " + dataType + " for Database Type " + response.getClass().getSimpleName());
                }
                return retVal;
        }

        public static String getVal(String dataType, AnonymousIpResponse response) throws IOException, GeoIp2Exception {
                Boolean retVal = false;
                switch (dataType) {
                        case "IS_ANONYMOUS":
                                retVal = response.isAnonymous();
                                break;
                        case "IS_ANONYMOUS_VPN":
                                retVal = response.isAnonymousVpn();
                                break;
                        case "IS_ISP":
                                retVal = response.isHostingProvider();
                                break;
                        case "IS_PUBLIC_PROXY":
                                retVal = response.isPublicProxy();
                                break;
                        case "IS_TOR_EXIT_NODE":
                                retVal = response.isTorExitNode();
                                break;
                        default:
                                throw new UnsupportedOperationException("Unable get " + dataType + " for Database Type " + response.getClass().getSimpleName());
                }
                return retVal ? "true" : "false";
        }

        public static String getVal(String dataType, DomainResponse response) throws IOException, GeoIp2Exception {
                if (dataType.equals("DOMAIN")) {
                        return response.getDomain();
                }
                else {
                        throw new UnsupportedOperationException("Unable get " + dataType + " for Database Type " + response.getClass().getSimpleName());
                }
        }

        public static String getVal(String dataType, ConnectionTypeResponse response) throws IOException, GeoIp2Exception {
                if (dataType.equals("CONNECTION")) {
                        return response.getConnectionType().toString();
                }
                else {
                        throw new UnsupportedOperationException("Unable get " + dataType + " for Database Type " + response.getClass().getSimpleName());
                }
        }

        /**
         * Get the String to be displayed in explain.
         *
         * @return The display string.
         */
        @Override
        public String getDisplayString(String[] children) {
                assert (children.length == 3);
                return "_FUNC_( " + children[0] + ", " + children[1] + ", " + children[2] + " )";
        }
}
