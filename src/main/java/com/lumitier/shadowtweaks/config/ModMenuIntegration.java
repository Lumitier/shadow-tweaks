package com.lumitier.shadowtweaks.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Haengt das Zahnrad in der Mod-Liste an unseren YACL-Screen. */
public class ModMenuIntegration implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ShadowTweaksScreen::create;
	}
}
