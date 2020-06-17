package fr.pilato.elasticsearch.tools.pipeline;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.pilato.elasticsearch.tools.ResourceList;
import fr.pilato.elasticsearch.tools.SettingsFinder;
import fr.pilato.elasticsearch.tools.template.TemplateFinder;

/**
 * Findes ingest pipelines on the classpath.
 * 
 * @author hjk181
 *
 */
public class PipelineFinder extends SettingsFinder {

	private static final Logger logger = LoggerFactory.getLogger(TemplateFinder.class);

	/**
	 * Find all pipelines in default classpath dir
	 * 
	 * @return a list of pipelines
	 * @throws IOException
	 *         if connection with elasticsearch is failing
	 * @throws URISyntaxException
	 *         this should not happen
	 */
	public static List<String> findPipelines() throws IOException, URISyntaxException {
		return findPipelines(Defaults.ConfigDir);
	}

	/**
	 * Find all pipelines
	 * 
	 * @param root
	 *        dir within the classpath
	 * @return a list of pipelines
	 * @throws IOException
	 *         if connection with elasticsearch is failing
	 * @throws URISyntaxException
	 *         this should not happen
	 */
	public static List<String> findPipelines(String root) throws IOException, URISyntaxException {
		if (root == null) {
			return findPipelines();
		}

		logger.debug("Looking for pipelines in classpath under [{}].", root);

		final List<String> pipelineNames = new ArrayList<>();
		String[] resources = ResourceList.getResources(root + "/" + Defaults.PipelineDir + "/"); // "es/_pipeline/"
		for (String resource : resources) {
			if (!resource.isEmpty()) {
				String withoutIndex = resource.substring(resource.indexOf("/") + 1);
				String template = withoutIndex.substring(0, withoutIndex.indexOf(Defaults.JsonFileExtension));
				logger.trace(" - found [{}].", template);
				pipelineNames.add(template);
			}
		}

		return pipelineNames;
	}

}
