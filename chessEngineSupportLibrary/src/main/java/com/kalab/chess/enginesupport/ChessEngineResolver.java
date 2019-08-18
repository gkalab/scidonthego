/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kalab.chess.enginesupport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChessEngineResolver {

	private static final String ENGINE_PROVIDER_MARKER = "intent.chess.provider.ENGINE";
	/** marker used to mark engines which need license checking */
	private static final String ENGINE_PROVIDER_LICENSE_MARKER = "intent.chess.provider.ACTIVATION";
	private static final String TAG = ChessEngineResolver.class.getSimpleName();
	private Context context;
	private String target;
	/** map of package -> activity for license checks */
	private Map<String, String> licenseCheckActivities = new HashMap<>();

	@SuppressWarnings("deprecation")
	public ChessEngineResolver(Context context) {
		super();
		this.context = context;
		this.target = Build.CPU_ABI; // use Build.SUPPORTED_ABIS[0] from API level 21 onwards
		sanitizeArmV6Target();
	}

	private void sanitizeArmV6Target() {
		if (this.target.startsWith("armeabi-v6")) {
			this.target = "armeabi";
		}
	}

	/**
	 * Return the list of all engines provided for the current target system
	 *
	 * @return List<ChessEngine> of engines provided
	 */
	public List<ChessEngine> resolveEngines() {
		resolveLicenseCheckActivitiesPerPackage();
		List<ChessEngine> result = new ArrayList<>();
		final Intent engineProviderIntent = new Intent(ENGINE_PROVIDER_MARKER);
		List<ResolveInfo> engineProviderList = context.getPackageManager()
				.queryIntentActivities(engineProviderIntent,
						PackageManager.GET_META_DATA);
		for (ResolveInfo resolveInfo : engineProviderList) {
			String packageName = resolveInfo.activityInfo.packageName;
			result = resolveEnginesForPackage(result, resolveInfo, packageName);
		}
		return result;
	}

	/**
	 * Resolve all the license check activities and put them into a map for
	 * later retrieval.
	 */
	private void resolveLicenseCheckActivitiesPerPackage() {
		final Intent engineLicenseIntent = new Intent(
				ENGINE_PROVIDER_LICENSE_MARKER);
		List<ResolveInfo> engineLicenseProviderList = context
				.getPackageManager().queryIntentActivities(engineLicenseIntent,
						PackageManager.GET_META_DATA);
		for (ResolveInfo resolveInfo : engineLicenseProviderList) {
			String packageName = resolveInfo.activityInfo.packageName;
			if (packageName != null) {
				ActivityInfo activityInfo = resolveInfo.activityInfo;
				licenseCheckActivities.put(packageName, activityInfo.name);
			}
		}
	}

	private List<ChessEngine> resolveEnginesForPackage(
			List<ChessEngine> result, ResolveInfo resolveInfo,
			String packageName) {
		if (packageName != null) {
			Log.d(TAG, "found engine provider, packageName=" + packageName);
			Bundle bundle = resolveInfo.activityInfo.metaData;
			if (bundle != null) {
				String authority = bundle
						.getString("chess.provider.engine.authority");
				Log.d(TAG, "authority=" + authority);
				if (authority != null) {
					try {
						Resources resources = context
								.getPackageManager()
								.getResourcesForApplication(
										resolveInfo.activityInfo.applicationInfo);
						int resId = resources.getIdentifier("enginelist",
								"xml", packageName);
						XmlResourceParser parser = resources.getXml(resId);
						parseEngineListXml(parser, authority, result,
								packageName);
					} catch (NameNotFoundException e) {
						Log.e(TAG, e.getLocalizedMessage(), e);
					}
				}
			}
		}
		return result;
	}

	private void parseEngineListXml(XmlResourceParser parser, String authority,
									List<ChessEngine> result, String packageName) {
		try {
			int eventType = parser.getEventType();
			while (eventType != XmlResourceParser.END_DOCUMENT) {
				try {
					if (eventType == XmlResourceParser.START_TAG) {
						addEngine(result, parser, authority, packageName);
					}
					eventType = parser.next();
				} catch (IOException e) {
					Log.e(TAG, e.getLocalizedMessage(), e);
				}
			}
		} catch (XmlPullParserException e) {
			Log.e(TAG, e.getLocalizedMessage(), e);
		}
	}

	private void addEngine(List<ChessEngine> result, XmlResourceParser parser,
						   String authority, String packageName) {
		if (parser.getName().equalsIgnoreCase("engine")) {
			String fileName = parser.getAttributeValue(null, "filename");
			String title = parser.getAttributeValue(null, "name");
			String targetSpecification = parser.getAttributeValue(null,
					"target");
			String[] targets = targetSpecification.split("\\|");
			for (String cpuTarget : targets) {
				if (target.equals(cpuTarget)) {
					int versionCode = 0;
					try {
						versionCode = context.getPackageManager()
								.getPackageInfo(packageName, 0).versionCode;
					} catch (NameNotFoundException e) {
						Log.e(TAG, e.getMessage());
					}
					try {
						ApplicationInfo app = context.getPackageManager().getApplicationInfo(packageName, 0);
						String enginePath = new File(app.nativeLibraryDir, fileName).getAbsolutePath();
						result.add(new ChessEngine(title, fileName, enginePath, authority,
								packageName, versionCode, licenseCheckActivities
								.get(packageName)));
					} catch (NameNotFoundException e) {
						Log.e(TAG, e.getLocalizedMessage(), e);
					}
				}
			}
		}
	}

	/**
	 * Check the license of an engine.
	 *
	 * @param caller
	 *            the activity which makes the license check
	 * @param requestCode
	 *            if >= 0, this code will be returned in onActivityResult() when the license check exits
	 * @param fileName
	 *            the file name of the engine
	 * @param packageName
	 *            the package name of the engine
	 * @return true if a license check is performed, false if there is no need for a license check.
	 *            If a license check is performed the caller must check the result in onActivityResult()
	 */
	public boolean checkLicense(Activity caller, int requestCode,
								String fileName, String packageName) {
		Log.d(TAG, "checking license for engine " + fileName + ", "
				+ packageName);
		for (ChessEngine engine : resolveEngines()) {
			if (engine.getPackageName().equals(packageName)
					&& engine.getFileName().equals(fileName)) {
				return engine.checkLicense(caller, requestCode);
			}
		}
		return false;
	}

	/**
	 * Don't use this in production - this method is only for testing. Set the
	 * cpu target.
	 *
	 * @param target
	 *            the cpu target to set
	 */
	public void setTarget(String target) {
		this.target = target;
		sanitizeArmV6Target();
	}
}
