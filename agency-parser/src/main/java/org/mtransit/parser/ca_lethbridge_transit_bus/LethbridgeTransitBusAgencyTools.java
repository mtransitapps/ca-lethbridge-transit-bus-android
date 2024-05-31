package org.mtransit.parser.ca_lethbridge_transit_bus;

import static org.mtransit.commons.RegexUtils.DIGITS;
import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRouteSNToIDConverter;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// http://opendata.lethbridge.ca/
// http://opendata.lethbridge.ca/datasets/e5ce3aa182114d66926d06ba732fb668
// https://www.lethbridge.ca/OpenDataSets/GTFS_Transit_Data.zip
public class LethbridgeTransitBusAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new LethbridgeTransitBusAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "Lethbridge Transit";
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	private static final String AGENCY_COLOR_BLUE_LIGHT = "009ADE"; // BLUE LIGHT (from web site CSS)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE_LIGHT;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String fixColor(@Nullable String color) {
		if ("80FF00".equalsIgnoreCase(color)) { // too light
			return "81CC2B"; // darker (from PDF schedule)
		} else if ("73CFFF".equalsIgnoreCase(color)) { // too light
			return "76AFE3"; // darker (from PDF schedule)
		} else if ("80FF80".equalsIgnoreCase(color)) { // too light
			return "4DB8A4"; // darker (from PDF schedule)
		}
		return super.fixColor(color);
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		//noinspection RedundantIfStatement
		if (routeId == 20L + 14L * MRouteSNToIDConverter.PREVIOUS // 20N
				|| routeId == 22L + 14L * MRouteSNToIDConverter.PREVIOUS // 20N
				|| routeId == 22L + 19L * MRouteSNToIDConverter.PREVIOUS // 20N
		) {
			return true; // OVERRIDE provided direction_id INVALID (used for "via " / trip variation)
		}
		return super.directionSplitterEnabled(routeId);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern ENDS_WITH_LOOP = Pattern.compile("([\\s]*loop$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern ENDS_WITH_ROUTE = Pattern.compile("([\\s]*route$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = ENDS_WITH_LOOP.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = ENDS_WITH_ROUTE.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.fixMcXCase(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanBounds(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private String[] getIgnoredWords() {
		return new String[]{"ATB"};
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = CleanUtils.fixMcXCase(gStopName);
		gStopName = CleanUtils.CLEAN_AND.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		if (CharUtils.isDigitsOnly(gStop.getStopCode())) {
			return Integer.parseInt(gStop.getStopCode()); // use stop code as stop ID
		}
		final Matcher matcher = DIGITS.matcher(gStop.getStopCode());
		if (matcher.find()) {
			int digits = Integer.parseInt(matcher.group());
			if (gStop.getStopCode().startsWith("MESC")) {
				return 13_050_000 + digits;
			}
		}
		throw new MTLog.Fatal("Unexpected stop ID for %s!", gStop);
	}
}
