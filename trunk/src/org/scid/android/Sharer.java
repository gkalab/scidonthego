package org.scid.android;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;

public class Sharer {

	private ComponentName caller;
	private Activity context;
	private Intent intent = null;

	public Sharer(Activity context, ComponentName caller) {
		this.context = context;
		this.caller = caller;
	}

	public void createShareIntent(String data) {
		// put all possible intents into one list
		List<Intent> targets = getTargetIntentsForType(
				"application/x-chess-pgn", data, caller);
		if (!targets.isEmpty()) {
			List<Intent> extraTargets = getTargetIntentsForType("text/plain",
					data, caller);
			Intent firstIntent = targets.get(targets.size() - 1);
			Intent chooser = Intent.createChooser(firstIntent, context
					.getResources().getText(R.string.menu_share_game));
			if (!extraTargets.isEmpty()) {
				// add all extra targets if they are not already in the list
				for (Intent extra : extraTargets) {
					boolean found = false;
					for (Intent target : targets) {
						if (target.getPackage().equals(extra.getPackage())) {
							found = true;
							break;
						}
					}
					if (!found) {
						targets.add(extra);
					}
				}
				targets.remove(firstIntent);
				chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,
						targets.toArray(new Parcelable[] {}));
			}
			this.intent = chooser;
		} else {
			Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_TEXT, data);
			this.intent = Intent.createChooser(shareIntent, context
					.getResources().getText(R.string.menu_share_game));
		}
	}

	private List<Intent> getTargetIntentsForType(String type, String data,
			ComponentName caller) {
		PackageManager packageManager = this.context.getPackageManager();
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType(type);
		List<Intent> targetedShareIntents = new ArrayList<Intent>();
		List<ResolveInfo> resInfo = packageManager.queryIntentActivityOptions(
				caller, null, shareIntent, PackageManager.MATCH_DEFAULT_ONLY);
		for (ResolveInfo resolveInfo : resInfo) {
			Intent targetedShareIntent = new Intent(Intent.ACTION_SEND);
			targetedShareIntent.setType(type);
			targetedShareIntent.putExtra(android.content.Intent.EXTRA_TEXT,
					data);
			targetedShareIntent
					.setPackage(resolveInfo.activityInfo.packageName);
			targetedShareIntents.add(targetedShareIntent);
		}
		return targetedShareIntents;
	}

	public Intent getIntent() {
		return intent;
	}
}
