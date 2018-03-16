package org.ayakaji.core;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.pmi.stat.WSBoundedRangeStatistic;
import com.ibm.websphere.pmi.stat.WSCountStatistic;
import com.ibm.websphere.pmi.stat.WSRangeStatistic;
import com.ibm.websphere.pmi.stat.WSStats;
import com.ibm.websphere.pmi.stat.WSTimeStatistic;

/**
 * Util for converting Java object to JSON string.
 * 
 * 
 */
public final class JsonUtil
{
  /**
   * Convert a Java object to JSON string.
   */
  @SuppressWarnings("unchecked")
    public static String toJson(Object o) {
      if (o==null)
        // nodeagent doesn't have servletSessionsModule, will return null.
        return "null";
        //return null;
      if (o instanceof String)
        return string2Json((String)o);
      if (o instanceof Boolean)
        return boolean2Json((Boolean)o);
      if (o instanceof Number)
        return number2Json((Number)o);
      if (o instanceof Map)
        return map2Json((Map<String, Object>)o);

      if (o instanceof WSStats[] )
        return wsStatsArray2Json( (WSStats[])o );

      if (o instanceof Object[])
        return array2Json((Object[])o);
      if (o instanceof int[])
        return intArray2Json((int[])o);
      if (o instanceof boolean[])
        return booleanArray2Json((boolean[])o);
      if (o instanceof long[])
        return longArray2Json((long[])o);
      if (o instanceof float[])
        return floatArray2Json((float[])o);
      if (o instanceof double[])
        return doubleArray2Json((double[])o);
      if (o instanceof short[])
        return shortArray2Json((short[])o);
      if (o instanceof byte[])
        return byteArray2Json((byte[])o);

      if (o instanceof WSStats )
        return wsStats2Json( (WSStats)o );
      if (o instanceof WSBoundedRangeStatistic )
        return wsBoundedStatistic2Json( (WSBoundedRangeStatistic)o );
      if (o instanceof WSRangeStatistic )
        return wsRangeStatistic2Json( (WSRangeStatistic)o );
      if (o instanceof WSCountStatistic )
        return wsCountStatistic2Json( (WSCountStatistic)o );
      if (o instanceof WSTimeStatistic )
        return wsTimeStatistic2Json( (WSTimeStatistic)o );

      //if (o instanceof Object)
      //    return object2Json( o );
      throw new RuntimeException("Unsupported type: " + o.getClass().getName());
    }

  static String wsBoundedStatistic2Json( WSBoundedRangeStatistic object )
  {
    StringBuilder sb = new StringBuilder();
    //sb.append( "\"name\":\"" );
    //sb.append( "\"attr\":{\"name\":\"" );
    //sb.append( "{\"name\":\"" );
    sb.append( '\"' );
    sb.append( object.getName() );
    sb.append('\"');
    //sb.append(',');
    //sb.append('\"');
    sb.append( ":{");
    sb.append( "\"value\":\"");
    sb.append( toJson( object.getCurrent()) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"lowerBound\":\"");
    sb.append( toJson( object.getLowerBound() ) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"upperBound\":\"");
    sb.append( toJson( object.getUpperBound() ) );
    sb.append('\"');
    sb.append( '}' );
    //sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }

  static String wsRangeStatistic2Json( WSRangeStatistic object )
  {
    StringBuilder sb = new StringBuilder();
    //sb.append( "\"name\":\"");
    //sb.append( "\"attr\":{\"name\":\"" );
    //sb.append( "{\"name\":\"" );
    sb.append('\"');
    sb.append( object.getName() );
    sb.append('\"');
    sb.append( ":{" );
    //sb.append(',');
    sb.append( "\"value\":\"");
    sb.append( toJson( object.getCurrent()) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"lowWaterMark\":\"");
    sb.append( toJson( object.getLowWaterMark() ) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"highWaterMark\":\"");
    sb.append( toJson( object.getHighWaterMark() ) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"integral\":\"");
    sb.append( toJson( object.getIntegral() ) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"mean\":\"");
    sb.append( toJson( object.getMean() ) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"startTime\":\"");
    sb.append( toJson( object.getStartTime() ) );
    sb.append('\"');
    sb.append(',');
    sb.append( "\"lastSampleTime\":\"");
    sb.append( toJson( object.getLastSampleTime() ) );
    sb.append('\"');
    sb.append( '}' );
    //sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }

  static String wsCountStatistic2Json( WSCountStatistic object )
  {
    StringBuilder sb = new StringBuilder();
    //sb.append( "\"attr\":{\"name\":\"" );
    //sb.append( "{\"name\":\"" );
    sb.append( "\"" );
    sb.append( object.getName() );
    sb.append('\"');
    //sb.append(',');
    sb.append( ":{");
    sb.append( "\"count\":\"" );
    sb.append( toJson( object.getCount()) );
    sb.append('\"');
    sb.append( '}' );
    //sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }

  static String wsTimeStatistic2Json( WSTimeStatistic object )
  {
    StringBuilder sb = new StringBuilder();
    //sb.append( "\"attr\":{\"name\":\"" );
    //sb.append( "{\"name\":\"" );
    sb.append( "\"" );
    sb.append( object.getName() );
    sb.append('\"');
    //sb.append(',');
    sb.append( ":{");
    sb.append( "\"totalTime\":\"" );
    sb.append( toJson( object.getTotalTime() ) );
    sb.append('\"');

    sb.append(',');
    sb.append( "\"min\":\"" );
    sb.append( toJson( object.getMinTime() ) );
    sb.append('\"');

    sb.append(',');
    sb.append( "\"max\":\"" );
    sb.append( toJson( object.getMaxTime() ) );
    sb.append('\"');

    sb.append( '}' );
    //sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }

  static String wsStats2Json( WSStats object)
  {
    /*
    if ( "nodeagent".equals( object.getName() ) )
    {
	return null;
    }
    */
    StringBuilder sb = new StringBuilder();
    //sb.append( "{\"name\":\"");
    //sb.append( "{\"");
    sb.append( "\"");
    sb.append( object.getName() );
    sb.append( "\":" );
    //sb.append( "{\"type\":\"");
    //sb.append( object.getStatsType() );
    //sb.append('\"');
    //sb.append(',');
    //sb.append( "\"Stat\":");
    sb.append( toJson( object.getStatistics()) );
    //sb.append(',');

    // if lenght = 0, don't output sub stats.
    WSStats[] subStats =  object.getSubStats(); 
    if ( subStats.length > 0 )
    {
      //sb.append( toJson( subStats ) );
      sb.append(',');
      for ( Object o : subStats )
      {
        //sb.append('%');
        // only one level sub array will be ok.
        sb.append( toJson(o) );
        //sb.append('?');
        sb.append(',');
      }
      // set last ',' to ''.
      //sb.setCharAt(sb.length()-1, '*');
      sb.setCharAt(sb.length()-1, ' ');
    }

    //sb.append( '}' );
    //sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }

  // @test: use {} instead of array [].
  static String wsStatsArray2Json( WSStats[] array )
  {
    if (array.length==0)
      //return "[]";
      return "{}";
    StringBuilder sb = new StringBuilder(array.length << 4);
    //sb.append('[');
    sb.append('{');
    for (WSStats o : array) 
    {
      sb.append(toJson(o));
      sb.append(',');
    }
    // set last ',' to ']':
    //sb.setCharAt(sb.length()-1, ']');
    sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }


  static String object2Json(Object object)
  {
    return "\"" + object.toString() + "\"";
    //return toJson( object );
  }

  static String array2Json(Object[] array) {
    if (array.length==0)
      //return "[]";
      return "{}";
    StringBuilder sb = new StringBuilder(array.length << 4);
    //sb.append('[');
    sb.append('{');
    for (Object o : array) {
      sb.append(toJson(o));
      sb.append(',');
    }
    // set last ',' to ']':
    //sb.setCharAt(sb.length()-1, ']');
    sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }

  static String intArray2Json(int[] array) {
    if (array.length==0)
      return "[]";
    StringBuilder sb = new StringBuilder(array.length << 4);
    sb.append('[');
    for (int o : array) {
      sb.append(Integer.toString(o));
      sb.append(',');
    }
    // set last ',' to ']':
    sb.setCharAt(sb.length()-1, ']');
    return sb.toString();
  }

  static String longArray2Json(long[] array) {
    if (array.length==0)
      return "[]";
    StringBuilder sb = new StringBuilder(array.length << 4);
    sb.append('[');
    for (long o : array) {
      sb.append(Long.toString(o));
      sb.append(',');
    }
    // set last ',' to ']':
    sb.setCharAt(sb.length()-1, ']');
    return sb.toString();
  }

  static String booleanArray2Json(boolean[] array) {
    if (array.length==0)
      return "[]";
    StringBuilder sb = new StringBuilder(array.length << 4);
    sb.append('[');
    for (boolean o : array) {
      sb.append(Boolean.toString(o));
      sb.append(',');
    }
    // set last ',' to ']':
    sb.setCharAt(sb.length()-1, ']');
    return sb.toString();
  }

  static String floatArray2Json(float[] array) {
    if (array.length==0)
      return "[]";
    StringBuilder sb = new StringBuilder(array.length << 4);
    sb.append('[');
    for (float o : array) {
      sb.append(Float.toString(o));
      sb.append(',');
    }
    // set last ',' to ']':
    sb.setCharAt(sb.length()-1, ']');
    return sb.toString();
  }

  static String doubleArray2Json(double[] array) {
    if (array.length==0)
      return "[]";
    StringBuilder sb = new StringBuilder(array.length << 4);
    sb.append('[');
    for (double o : array) {
      sb.append(Double.toString(o));
      sb.append(',');
    }
    // set last ',' to ']':
    sb.setCharAt(sb.length()-1, ']');
    return sb.toString();
  }

  static String shortArray2Json(short[] array) {
    if (array.length==0)
      return "[]";
    StringBuilder sb = new StringBuilder(array.length << 4);
    sb.append('[');
    for (short o : array) {
      sb.append(Short.toString(o));
      sb.append(',');
    }
    // set last ',' to ']':
    sb.setCharAt(sb.length()-1, ']');
    return sb.toString();
  }

  static String byteArray2Json(byte[] array) {
    if (array.length==0)
      return "[]";
    StringBuilder sb = new StringBuilder(array.length << 4);
    sb.append('[');
    for (byte o : array) {
      sb.append(Byte.toString(o));
      sb.append(',');
    }
    // set last ',' to ']':
    sb.setCharAt(sb.length()-1, ']');
    return sb.toString();
  }

  static String map2Json(Map<String, Object> map) 
  {
    if (map.isEmpty())
      return "{}";
    StringBuilder sb = new StringBuilder(map.size() << 4);
    sb.append('{');
    Set<String> keys = map.keySet();
    for (String key : keys) 
    {
      //
      // ignore nodeagent, null will return, no servletSessionModule.
      //
      if ( "nodeagent".equals( key ) )
      {
        continue;
      }
      Object value = map.get(key);
      sb.append('\"');
      sb.append(key);
      sb.append('\"');
      sb.append(':');
      sb.append(toJson(value));
      sb.append('\n');
      sb.append(',');
    }
    // set last ',' to '}':
    sb.setCharAt(sb.length()-1, '}');
    return sb.toString();
  }

  static String boolean2Json(Boolean bool) {
    return bool.toString();
  }

  static String number2Json(Number number) {
    return number.toString();
  }

  static String string2Json(String s) {
    StringBuilder sb = new StringBuilder(s.length()+20);
    sb.append('\"');
    for (int i=0; i<s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\"':
          sb.append("\\\"");
        break;
        case '\\':
        sb.append("\\\\");
        break;
        case '/':
        sb.append("\\/");
        break;
        case '\b':
        sb.append("\\b");
        break;
        case '\f':
        sb.append("\\f");
        break;
        case '\n':
        sb.append("\\n");
        break;
        case '\r':
        sb.append("\\r");
        break;
        case '\t':
        sb.append("\\t");
        break;
        default:
        sb.append(c);
      }
    }
    sb.append('\"');
    return sb.toString();
  }
}
