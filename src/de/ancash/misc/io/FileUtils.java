package de.ancash.misc.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import de.ancash.libs.org.simpleyaml.configuration.ConfigurationSection;
import de.ancash.libs.org.simpleyaml.configuration.file.YamlFile;
import de.ancash.libs.org.simpleyaml.exceptions.InvalidConfigurationException;

public class FileUtils {

	public static void move(YamlFile file, String from, String to) {
		if (!file.contains(from))
			return;
		if (from.equals(to))
			return;
		if (file.isConfigurationSection(from)) {
			moveConfigurationSection(file, from, to);
			return;
		}
		file.set(to, file.get(from));
		file.remove(from);
		System.out.println(String.format("Moved '%s' in '%s' to '%s'", from, file.getFilePath(), to));
	}

	public static void moveConfigurationSection(YamlFile file, String from, String to) {
		if (from.equals(to))
			return;
		if (!file.isConfigurationSection(from)) {
			move(file, from, to);
			return;
		}
		System.out.println(
				String.format("Moving ConfigurationSection '%s' in '%s' to '%s'", from, file.getFilePath(), to));
		ConfigurationSection section = file.getConfigurationSection(from);
		for (String key : section.getKeys(false)) {
			moveConfigurationSection(file, from + "." + key, to + "." + key);
		}
		if (section.isEmpty())
			file.remove(from);
	}

	public static void setMissingConfigurationSections(YamlFile original, InputStream src)
			throws IOException, InvalidConfigurationException {
		setMissingConfigurationSections(original, src, new HashSet<>());
	}

	public static void setMissingConfigurationSections(YamlFile original, InputStream src,
			Set<String> ignoreSectionIfSectionContains) throws IOException, InvalidConfigurationException {
		File file = new File(System.nanoTime() + ".tmp");
		try {
			YamlFile srcYaml = new YamlFile(file);
			srcYaml.createNewFile();
			de.ancash.libs.org.apache.commons.io.FileUtils.copyInputStreamToFile(src, srcYaml.getConfigurationFile());
			setMissingConfigurationSections(original, srcYaml, ignoreSectionIfSectionContains);
		} finally {
			file.delete();
		}
	}

	public static void setMissingConfigurationSections(YamlFile original, YamlFile src)
			throws InvalidConfigurationException, IOException {
		setMissingConfigurationSections(original, src, new HashSet<>());
	}

	public static void setMissingConfigurationSections(YamlFile original, YamlFile src,
			Set<String> ignoreSectionIfSectionContains) throws InvalidConfigurationException, IOException {
		original.loadWithComments();
		src.loadWithComments();
		for (String key : src.getKeys(false))
			compute(original, ignoreSectionIfSectionContains, key, src);
		original.save();
		src.save();
	}

	private static void compute(YamlFile original, Set<String> ignoreSectionIfSectionContains, String key,
			ConfigurationSection curSection) {
		if (curSection.isConfigurationSection(key)) {
			ConfigurationSection value = curSection.getConfigurationSection(key);

			if (!original.contains(value.getCurrentPath()))
				set(original, curSection, key);
			else {
				for (String keys : curSection.getKeys(false))
					if (ignoreSectionIfSectionContains.contains(keys))
						return;

				for (String keys : value.getKeys(false))
					compute(original, ignoreSectionIfSectionContains, keys, value);
			}
		} else {
			String path = curSection.getParent() == null ? key : curSection.getCurrentPath() + "." + key;

			if (!original.contains(path)) {

				for (String keys : curSection.getKeys(false)) {
					if (!ignoreSectionIfSectionContains.contains(keys))
						continue;
					if (curSection.getParent() == null) {
						if (original.contains(keys))
							return;
					} else {
						if (original.contains(curSection.getCurrentPath() + "." + keys))
							return;
					}
				}
				set(original, curSection, key);
			}
		}
	}

	private static void set(YamlFile file, ConfigurationSection section, String key) {
		String path = "".equals(section.getCurrentPath()) ? key : section.getCurrentPath() + "." + key;

		if (section.isConfigurationSection(key)) {
			file.getConfigurationSection(section.getCurrentPath()).createSection(key,
					section.getConfigurationSection(key).getMapValues(true));
			System.out.println(String.format("Could not find key '%s' in '%s'. Set to '%s'", path, file.getFilePath(),
					section.getConfigurationSection(key).getMapValues(true)));
		} else {
			file.set(path, section.get(key));
			System.out.println(String.format("Could not find key '%s' in '%s'. Set to '%s'", path, file.getFilePath(),
					section.get(key)));
		}
	}
}