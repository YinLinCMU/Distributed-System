package core;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import util.Configuration;
import util.Node;
import util.Rule;

/*
 * Parse the informationg form config.yaml
 */
public class YalmParser{

	public static Configuration parse(String file) throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(file));
		Constructor constructor = new Constructor(Configuration.class);
		TypeDescription confDes = new TypeDescription(Configuration.class);
		confDes.putListPropertyType("configuration", Node.class);
		confDes.putListPropertyType("sendRules", Rule.class);
		confDes.putListPropertyType("receiveRules", Rule.class);
		constructor.addTypeDescription(confDes);
		Yaml yaml = new Yaml(constructor);
		return (Configuration)yaml.load(input);
	}
}