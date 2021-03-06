/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.liferay.lugbot.custom.springmvcportlet;

import com.liferay.lugbot.api.LugbotConfig;
import com.liferay.lugbot.api.ProposalDTO;
import com.liferay.lugbot.api.UpgradeProvider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.liferay.lugbot.api.util.GitFunctions.commitChanges;
import static com.liferay.lugbot.api.util.GitFunctions.getCurrentBranchName;
import static com.liferay.lugbot.api.util.LogFunctions.logError;

/**
 * @author Rafael Oliveira
 */
@Component(name = "spring-mvc-portlet-migrate-code")
@ServiceRanking(22000)
public class SpringMVCPortletMigradeCodeProvider implements UpgradeProvider {

	@Override
	public List<String> computePossibleUpgrades(Path repoPath, LugbotConfig lugbotConfig) {
		return Collections.singletonList("SpringMVCPortletMigradeCode");
	}

	@Override
	public Optional<ProposalDTO> provideUpgrade(Path repoPath, LugbotConfig lugbotConfig, String upgradeName) {

		Path pluginsSDKPath = repoPath.resolve(lugbotConfig.tasks.upgrade.pluginsSDKPath);

		Path workspacePath = repoPath.resolve(lugbotConfig.tasks.upgrade.workspacePath);
		Path modulesPath = workspacePath.resolve("modules");

		List<String> pluginNames = lugbotConfig.tasks.upgrade.plugins;

		pluginNames.forEach(pluginName -> {
			try {
				Path from = pluginsSDKPath.resolve(pluginName);
				Path to = modulesPath.resolve(pluginName);
				_migrateCode(from, to);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		try {
			commitChanges(repoPath, "migrate spring-mvc code", Collections.singletonList("."));

			return Optional.of(
				new ProposalDTO(
					"SpringMVCPortletMigradeCode", "SpringMVCPortlet [Migrate Code]", "required",
					"SpringMVCPortlet [Migrate Code]", "", getCurrentBranchName(repoPath)));
		}
		catch (Exception e) {
			logError(_logger, e);
		}

		return Optional.empty();
	}

	private void _migrateCode(Path fromPath, Path toPath) {
		try {
			if (fromPath.toFile().isDirectory()) {
				_copyDirectoryRecursively(fromPath.toFile(), toPath.toFile());
			} else {
				Files.copy(fromPath, toPath, StandardCopyOption.REPLACE_EXISTING);
			}

		} catch (Exception e) {
			logError(_logger, e);
		}
	}

	private void _copyDirectoryRecursively(File fromFile, File toFile) throws IOException {
		if (!toFile.exists()) {
			toFile.mkdir();
		}
		for (String child : fromFile.list()) {
			_migrateCode(new File(fromFile, child).toPath(), new File(toFile, child).toPath());
		}
	}

	@Reference(service = org.osgi.service.log.LoggerFactory.class)
	private Logger _logger;

}
