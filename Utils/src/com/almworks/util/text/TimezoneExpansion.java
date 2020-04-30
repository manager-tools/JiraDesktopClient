package com.almworks.util.text;

import org.almworks.util.Collections15;
import org.almworks.util.Log;
import org.almworks.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class TimezoneExpansion {
  /**
   * Format:
   * target timezone ['!' daylight saving source tz], target timezone w/ daylight saving, .... , alternative timezones
   */
  private static final Object[][] CONVERSION = {
    {new ZoneHack("EST", "EDT", "US/Eastern"), "EST", "EDT", "Eastern", "US/Eastern", "ET"},
    {new ZoneHack("EST", "EDT", "Canada/Eastern"), "Canada/Eastern"},
    {new ZoneHack("CST", "CDT", "US/Central"), "CST", "CDT", "Central", "US/Central", "CT"},
    {new ZoneHack("CST", "CDT", "Canada/Central"), "Canada/Central"},
    {new ZoneHack("MST", "MDT", "US/Mountain"), "MST", "MDT", "Mountain", "US/Mountain", "MT"},
    {new ZoneHack("MST", "MDT", "Canada/Mountain"), "Canada/Mountain"},
    {new ZoneHack("PST", "PDT", "US/Pacific"), "PST", "PDT", "Pacific", "US/Pacific", "PT"},
    {new ZoneHack("PST", "PDT", "Canada/Pacific"), "Canada/Pacific"},
    {new ZoneHack("-1000", "-0900", "US/Hawaii"), "HAST", "HADT"},
    {new ZoneHack("-0900", "-0800", "US/Alaska"), "AKST", "AKDT"},
    {new ZoneHack("-0400", "-0300", "Brazil/West"), "WBT",},
    {new ZoneHack("-0330", "-0230", "Canada/Newfoundland"), "NST",},
    {new ZoneHack("-0300", "-0200", "Brazil/East"), "EBT",}, {new ZoneHack("UTC", null, null), "WAT",},
    {new ZoneHack("UTC", "+0100", "WET"), "WET", "WEST",}, {new ZoneHack("+0100", "+0200", "CET"), "CET", "CEST"},
    {new ZoneHack("+0200", "+0300", "EET"), "EET", "EEST"}, {new ZoneHack("+0200", null, null), "CAT", "SAST"},
    {new ZoneHack("+0300", null, null), "EAT",}, {new ZoneHack("+0300", "+0400", "Europe/Moscow"), "MSK", "MSD"},
    {new ZoneHack("+0330", null, null), "IRT",}, {new ZoneHack("+0700", null, null), "CXT",},
    {new ZoneHack("+0800", null, null), "AWST", "CCT"}, {new ZoneHack("+0900", null, null), "JST", "KST", "EIT",},
    {new ZoneHack("+0930", "+1030", "Australia/Adelaide"), "ACST", "ASDT",},
    {new ZoneHack("+1000", "+1100", "Australia/Sydney"), "AEST", "AEDT",},
    {new ZoneHack("+1130", "+1230", "Pacific/Norfolk"), "NFT",},};

  private static final Map<String, ZoneHack> ourTimezoneConversion = createConversion();

  /**
   * @param timezone    alternative timezone name to be converted
   * @param dateAsGmt   date in GMT to check for daylight saving
   * @param zoneBuffer  [1] array to receive actual time zone
   * @param rfcTimezone if not null, specified equivalent RFC timezone (format char: Z), to resolve conflicting timezones like CST
   * @return null if no correction is needed, or the new timezone
   */
  @Nullable
  public static String convertTimezone(String timezone, Date dateAsGmt, TimeZone[] zoneBuffer,
    @Nullable String rfcTimezone)
  {
    if (timezone == null || "GMT".equals(timezone))
      return null;

//  // this was used to check that the timezone is not recognized; we have to comment this now because we
//  // also want to change recognizable timezones like EST
//
//    TimeZone original = TimeZone.getTimeZone(timezone);
//    if (original != null && !"GMT".equals(original.getID()))
//      return null;

    // CST hack
    // #613
    if (rfcTimezone != null) {
      if ("CST".equalsIgnoreCase(timezone) && "+0800".equalsIgnoreCase(rfcTimezone)) {
        zoneBuffer[0] = TimeZone.getTimeZone("Asia/Taipei");
        return "CST";
      }
      if ("IST".equalsIgnoreCase(timezone) && "+0200".equalsIgnoreCase(rfcTimezone)) {
        zoneBuffer[0] = TimeZone.getTimeZone("Israel");
        return "IST";
      }
      if ("IST".equalsIgnoreCase(timezone) && "+0100".equalsIgnoreCase(rfcTimezone)) {
        zoneBuffer[0] = TimeZone.getTimeZone("Europe/Dublin");
        return "IST";
      }
      if (("EST".equalsIgnoreCase(timezone) || "EDT".equalsIgnoreCase(timezone)) &&
        ("+1100".equalsIgnoreCase(rfcTimezone) || "+1000".equalsIgnoreCase(rfcTimezone)))
      {
        zoneBuffer[0] = TimeZone.getTimeZone("Australia/Sydney");
        return timezone;
      }
    }

    ZoneHack zoneHack = ourTimezoneConversion.get(Util.upper(timezone));
    if (zoneHack == null) {
      return null;
    }

    String tz = zoneHack.dateTimezoneReplacement;
    if (zoneBuffer != null) {
      zoneBuffer[0] = zoneHack.timezone;
    }
    if (zoneHack.timezone != null) {
      // maybe correct timezone for daylight saving
      // convert bareDate from GMT to local time
      dateAsGmt = new Date(dateAsGmt.getTime() - zoneHack.timezone.getRawOffset());
      if (zoneHack.timezone.inDaylightTime(dateAsGmt)) {
        // use daylight saving time zone
        tz = zoneHack.dstTimezoneReplacement;
      }
    }
    return tz;
  }

  private static Map<String, ZoneHack> createConversion() {
    Map<String, ZoneHack> map = Collections15.hashMap();
    for (int i = 0; i < CONVERSION.length; i++) {
      Object[] variants = CONVERSION[i];
      assert variants.length >= 2;
      ZoneHack hack = (ZoneHack) variants[0];
      for (int j = 1; j < variants.length; j++) {
        String variant = Util.upper((String) variants[j]);
        ZoneHack expunged = map.put(variant, hack);
        if (expunged != null) {
          assert false : variant + ":" + expunged;
          Log.warn("expunged zone hack " + expunged);
        }
      }
    }
    return Collections.synchronizedMap(Collections.unmodifiableMap(map));
  }


  private static class ZoneHack {
    @NotNull
    public final String dateTimezoneReplacement;

    @Nullable
    public final String dstTimezoneReplacement;

    @Nullable
    public final TimeZone timezone;

    public ZoneHack(String dateReplacement, String dstReplacement, String timezoneId) {
      dateTimezoneReplacement = dateReplacement;
      dstTimezoneReplacement = dstReplacement;
      TimeZone tz = timezoneId == null ? null : TimeZone.getTimeZone(timezoneId);
      if (tz != null && "GMT".equals(tz.getID())) {
        tz = null;
      }
      timezone = tz;
    }

    public String toString() {
      return dateTimezoneReplacement + "/" + dstTimezoneReplacement + "/" +
        (timezone == null ? "null" : timezone.getID());
    }
  }

  // Source of information:
  //
//  protected static final String PAC = "Pacific";
//   protected static final String EUR = "Europe";
//   protected static final String ASIA = "Asia";
//   protected static final String EA = "East Asia";
//   protected static final String NA = "North America";
//   protected static final String SA = "South America";
//   protected static final String AFR = "Africa";
//   protected static final String AUS = "Australia";
//    /** list of all timezones as names.
//    *   <br />
//    *   Format: WINTER, SUMMER(Daylight Savings if appl.), REGION, ABBREV., NAME, CITIES
//    *   <br />
//    *   Please check the log after changes in this table
//    */    protected final static String[][] TIMEZONE_TABLE = {
//        { "UTC-12",   null,       PAC, "",           "","" },
//        { "UTC-11",   null,       PAC, "",           "","" },
//        { "UTC-10",   "UTC-9",    PAC, "HAST/HADT",  "Hawaii-Aleutian Standard/Daylight Time","" },
//        { "UTC-9",    "UTC-8",    NA,  "AKST/AKDT",  "Alaska Standard/Daylight Time","Anchorage" },  //  ok
//        { "UTC-8",    "UTC-7",    NA,  "PST/PDT",    "Pacific Standard/Daylight Time","Los Angeles, San Francisco, Seattle, Vancouver" },  //  ok
//        { "UTC-7",    null,       NA,  "",           "Arizona(US), Saskatchewan(Can)","Phoenix, Saskatoon" },
//        { "UTC-7",    "UTC-6",    NA,  "MST/MDT",    "Mountain Standard/Daylight Time","Denver, Albuquerque, Salt Lake City, Edmonton, Calgary" },  //  ok
//        { "UTC-6",    "UTC-5",    NA,  "CST/CDT",    "Central Standard/Daylight Time","Chicago, Houston, Winnipeg" },  //  ok
//        { "UTC-6",    "UTC-5",    NA,  "",           "Middle American Time","Mexico City, Managua(Nicaragua)" }, //  ok
//        { "UTC-6",    null,       NA,  "",           "Guatemala, Honduras, El Salvador, Costa Rica","" },
//        { "UTC-5",    "UTC-4",    NA,  "EST/EDT",    "Eastern Standard/Daylight Time","New York, Miami, Boston, Montreal, Ottawa, Atlanta" },  //  ok
//        { "UTC-5",    null,       SA,  "",           "","Quito(Equador), Lima(Peru), Bogota(Columbia)" },
//        { "UTC-5",    null,       NA,  "",           "Indiana Time + Cuba","Indianapolis, Habana" },
//        { "UTC-4",    null,       SA,  "",           "Western Brazilian Time (North)","Manaus" },  //  ok
//        { "UTC-4",    "UTC-3",    SA,  "WBT",        "Western Brazilian Time (South)","Cuiaba" },  //  ok
//        { "UTC-4",    null,       SA,  "",           "Atlantic Time SA","Caracas, La Paz" },  //  ok, Venezuela + Bolivien
//        { "UTC-4",    "UTC-3",    SA,  "",           "Chile","Santiago de Chile" },  //  ok
//        { "UTC-4",    "UTC-3",    NA,  "",           "Atlantic Time NA","Halifax" },
//        { "UTC-3:30", "UTC-2:30", NA,  "NST",        "Newfoundland Standard Time","" },
//        { "UTC-3",    null,       SA,  "",           "Eastern Brazilian Time (North)","Fortaleza" },
//        { "UTC-3",    "UTC-2",    SA,  "EBT",        "Eastern Brazilian Time (South)","Sao Paolo, Brasilia, Rio de Janeiro, Recife" },
//        { "UTC-3",    null,       SA,  "",           "Argentinia","Buenos Aires" },  //  ok
//        { "UTC-2",    null,       SA,  "",           "","" },
//        { "UTC",      null,       null,"UTC",        "Coordinated Universal Time","" },
//        { "UTC",      null,       AFR, "WAT",        "West African Time","Dakar, Timbuktu, Casablanca" },  //  ok
//        { "UTC",      null,       EUR, "",           "Island","Reykjavik" },  //  ok
//        { "UTC",      "UTC+1",    EUR, "WET/WEST",   "Western European (Summer) Time","Lisbon" },  //  ok
//        { "UTC",      "UTC+1",    EUR, "GMT/BST",    "Greenwich mean Time/British Summer Time","London" },  //  ok
//        { "UTC",      "UTC+1",    EUR, "GMT/IST",    "Greenwich mean Time/Irish Summer Time","Dublin" },  //  ok
//        { "UTC+1",    "UTC+2",    EUR, "CET/CEST",   "Central European (Summer) Time","Prague, Paris, Amsterdam, Rome, Berlin, Munich, Vienna, Oslo" },  //   ok
//        { "UTC+1",    null,       AFR, "",           "","Algiers,Tripoli, Lagos, Kinshasa, Lusaka" },
//        { "UTC+1",    "UTC+2",    AFR, "",           "","Tunis, Windhoek(Namibia)" },
//        { "UTC+2",    "UTC+3",    EUR, "EET/EEST",   "Eastern European (Summer) Time","Athens, Istanbul, Helsinki, Vilnius, Riga, Minsk, Kiev, Chisinau, Bucharest" },  //  ok
//        { "UTC+2",    "UTC+3",    AFR, "",           "","Jerusalem, Cairo" }, //  ok
//        { "UTC+2",    null,       AFR, "CAT",        "Central African Time","Khartoum, Harare" },  //  ok
//        { "UTC+2",    null,       AFR, "SAST",       "South African Standard Time","Capetown" },
//        { "UTC+3",    null,       AFR, "EAT",        "East African Time","Nairobi, Addis Ababa, Mombasa" },  //  ok
//        { "UTC+3",    "UTC+4",    EUR, "MSK",        "Moskow Time","" },  //  ok
//        { "UTC+3",    null,       ASIA,"",           "","Riyadh" },  //  ok
//        { "UTC+3",    "UTC+4",    ASIA,"",           "","Kuwait, Baghdad" },  //  ok
//        { "UTC+3:30", null,       ASIA,"IRT",        "Iran","Tehran" },
//        { "UTC+4",    null,       ASIA,"",           "","Abu Dhabi, Muscat" },
//        { "UTC+4",    "UTC+5",    ASIA,"",           "","Baku, Volgograd, Tblisi" },
//        { "UTC+4:30", null,       ASIA,"",           "Afghanistan","Kabul" },
//        { "UTC+5",    null,       ASIA,"",           "Pakistan","Tashkent, Orenburg, Swerdlowsk, Ashkhabad, Dushanbe, Karachi" },  //  ok
//        { "UTC+5",    "UTC+6",    ASIA,"",           "","Yekaterinburg" },  //  ok
//        { "UTC+5:30", null,       ASIA,"",           "India","Calcutta, Mumbai, New Delhi, Bangalore, Adaman Islands, Nicobar Islands" },  //  ok
//        { "UTC+5:45", null,       ASIA,"",           "Nepal","Kathmandu" },
//        { "UTC+6",    null,       ASIA,"",           "Bangla Desh","Colombo, Myanmar" },  //  ok
//        { "UTC+6",    "UTC+7",    ASIA,"",           "","Novosibirsk, Omsk" },
//        { "UTC+6:30", null,       ASIA,"",           "Myanmar","Rangoon, Cocos Is." },
//        { "UTC+7",    null,       AUS, "CXT",        "Christmas Island Time","" },
//        { "UTC+7",    null,       ASIA,"",           "Bangkok","Bangkok, Jakarta, Hanoi" },  //  ok
//        { "UTC+8",    null,       AUS, "AWST",       "Australian Western Standard Time","Perth" }, // (or WAST?)
//        { "UTC+8",    null,       EA,  "CCT",        "China Coast Time","Hong Kong, Beijing, Singapore, Shanghai, Peking" },  //  ok
//        { "UTC+8",    "UTC+9",    ASIA,"",           "","Ulan Bator, Irkutsk" },  //  ok
//        { "UTC+9",    null,       EA,  "JST",        "Japan Standard Time","Tokyo, Osaka" },  //  ok
//        { "UTC+9",    null,       EA,  "KST",        "Korean Standard Time","Seoul, Pyongyang" },  //  ok
//        { "UTC+9",    null,       EA,  "EIT",        "East Indonesia Time","" },
//        { "UTC+9",    "UTC+10",   ASIA,"",           "Russia","Yakutsk" },
//        { "UTC+9:30", null,       AUS, "",           "Northern Territory","Darwin" },  //  ok
//        { "UTC+9:30", "UTC+10:30",AUS, "ACST/ASDT",  "Australian Central Standard/Daylight Time","Adelaide" },  //  ok
//        { "UTC+10",   null,       AUS, "",           "Queensland","Cairns" },  //  ok
//        { "UTC+10",   "UTC+11",   AUS, "AEST/AEDT",  "Australian Eastern Standard/Daylight Time","Sydney, Melbourne" },  //  ok
//        { "UTC+10",   "UTC+11",   ASIA,"",           "Russia","Vladivostok" },
//        { "UTC+10:30","UTC+11:30",AUS, "",           "","Lord Howe Island" },
//        { "UTC+11",   "UTC+12",   ASIA,"",           "Russia","Magadan" },
//        { "UTC+11:30","UTC+12:30",AUS, "NFT",        "Norfolk","Norfolk Island" },
//        { "UTC+12",   "UTC+13",   ASIA,"",           "Russia","Kamchatka" },
//        { "UTC+12",   "UTC+13",   AUS, "",           "New Zealand","Auckland, Wellington" },
//        { "UTC+13",   "UTC+14",   ASIA,"",           "Russia","Rawaki Is." },
//        { "UTC+14",   null,       PAC, "",           "","" }
//    };
}
