import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class InputStreamTo {
	public static String str(InputStream input, String encoding) {
		char[] buffer = new char[1];
		StringBuilder sb = new StringBuilder();
		int len;
		try (Reader reader = new InputStreamReader(input, encoding)) {
			while ((len = reader.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, len));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	public static void pic(InputStream input, String filename) {
		if (input == null) {
			return;
		}
		byte[] buffer = new byte[1024];
		FileOutputStream output = null;
		int len;
		try {
			output = new FileOutputStream(filename);
			while ((len = input.read(buffer)) != -1) {
				output.write(buffer, 0, len);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				input.close();
				output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

public class Main {
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();

		URL url = new URL("https://pvp.qq.com/web201605/js/herolist.json");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestMethod("GET");
		conn.setConnectTimeout(6 * 1000);
		conn.setReadTimeout(6 * 1000);

		InputStream input = conn.getInputStream();
		String hero_json = InputStreamTo.str(input, "UTF-8");

		conn.disconnect();

		List<List<String>> hero_list = read_to_list(hero_json);

		use_threads_download(hero_list);

		long endTime = System.currentTimeMillis();
		long duration = endTime - startTime;
		System.out.println("spend %s seconds.".formatted(duration / 1000.0));
	}

	private static List<List<String>> read_to_list(String s) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
		List<List<String>> result = new ArrayList<>();
		try {
			JsonNode rootNode = objectMapper.readTree(s);
			for (JsonNode node : rootNode) {
				int ename = node.get("ename").asInt();
				String cname = node.get("cname").asText();
				String skin_name = node.get("skin_name").asText();
				String hero_url_prefix = "https://game.gtimg.cn/images/yxzj/img201606/skin/hero-info/%s/%s-bigskin-"
						.formatted(ename, ename);
				List<String> pair = new ArrayList<>();
				pair.add(cname);
				pair.add(skin_name);
				pair.add(hero_url_prefix);
				result.add(pair);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	private static void use_threads_download(List<List<String>> list) {
		int coreCount = Runtime.getRuntime().availableProcessors();

		Thread[] threads = new Thread[coreCount * 2];

		String dir_name = System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "wzry";
		File wzry_dir = new File(dir_name);
		if (!wzry_dir.isDirectory()) {
			wzry_dir.mkdir();
		}

		int size = list.size() / (coreCount * 2) + 1;
		int begin = 0;
		int end = 0;
		int i;
		for (i = 0; end < list.size(); i++) {
			begin = i * size;
			end = Math.min((i + 1) * size, list.size());
			threads[i] = new Download(list, dir_name, begin, end);
			threads[i].start();
		}

		for (Thread t : threads) {
			try {
				if (t != null) {
					t.join();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}

class Download extends Thread {
	private List<List<String>> list;
	String dirname;
	int begin;
	int end;

	Download(List<List<String>> list, String dirname, int begin, int end) {
		super();
		this.list = list;
		this.dirname = dirname;
		this.begin = begin;
		this.end = end;
	}

	@Override
	public void run() {
		for (int i = begin; i < end; i++) {
			// [廉颇, 正义爆轰|地狱岩魂|无尽征程|寅虎·御盾|功夫炙烤|撼地雄心,
			// https://game.gtimg.cn/images/yxzj/img201606/skin/hero-info/105/105-bigskin-]
			String hero_name = list.get(i).get(0);
			String skin_str = list.get(i).get(1);
			String[] skin_name = skin_str.split("\\|");
			String skin_url_prefix = list.get(i).get(2);

			String skin_dir = dirname + File.separator + hero_name;
			File hero_dir = new File(skin_dir);
			if (!hero_dir.isDirectory()) {
				hero_dir.mkdir();
			}

			for (int j = 0; j < skin_name.length; j++) {
				String skin_url = skin_url_prefix.concat("%d.jpg".formatted(j + 1));
				InputStream in = get_inputStream(skin_url);
				String pic_name = skin_dir.concat("%s%d %s.jpg".formatted(File.separator, j + 1, skin_name[j]));
				InputStreamTo.pic(in, pic_name);
			}
			System.out.println("%s-----%s-----下载完成".formatted(hero_name, skin_str));
		}
	}

	private InputStream get_inputStream(String s) {
		InputStream input = null;
		try {
			URL url = new URL(s);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setRequestMethod("GET");
			conn.setConnectTimeout(6 * 1000);
			conn.setReadTimeout(6 * 1000);

			input = conn.getInputStream();
		} catch (IOException e) {
			// e.printStackTrace();
		}

		return input;
	}

}