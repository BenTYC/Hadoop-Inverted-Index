package edu.ucr.cs.cs242;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InvertedIndex {
	public static class IndexMapper extends Mapper<Object, Text, Text, Text> {

		private Text valueInfo = new Text();
		private Text keyInfo = new Text();
		private String pattern = "(?i)[^a-zA-Z0-9\\u4E00-\\u9FA5]";

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

			String line = value.toString().replaceAll(pattern, " ");
			StringTokenizer itr = new StringTokenizer(line);
			
			int id = Integer.parseInt(itr.nextToken());
			int pos = 0;
			int lengthD = itr.countTokens();
			//System.out.println("Serial number:" + id);
			
			while (itr.hasMoreTokens()) {
				String token = itr.nextToken();

				// Stemming
				Stemmer s = new Stemmer();
				char temp[] = new char[token.length()];
				temp = token.toCharArray();
				s.add(temp, token.length());
				s.stem();

				keyInfo.set(s.toString().toLowerCase() + ":" + id + ":" + lengthD);
				valueInfo.set(String.valueOf(pos));
				
				context.write(keyInfo, valueInfo);  //<key|value> = <word:id:len|"1">
				pos++;
			}
		}
	}

	public static class IndexCombiner extends Reducer<Text, Text, Text, Text> {
		private Text info = new Text();

		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			// input：<key,value>---<"word:id:len",list of pos(0,4,9,11)>
			int sum = 0;
			String posList = "";
			for (Text val : values) {
				//sum += Integer.parseInt(val.toString());
				posList += val.toString() + ',';
				sum++;
			}
			
			String[] arr = key.toString().split(":");
			double tf = sum / Double.parseDouble(arr[2]);

			key.set(arr[0]);
			info.set(arr[1] + ":" + tf + ":" + posList);
			context.write(key, info);// output:<key|value>----<word|id:tf:posList>
		}
	}

	public static class IndexReducer extends Reducer<Text, Text, Text, Text> {
		private Text result = new Text();

		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			
			// input：<word,list("id1:tf1:posList1","id3:tf3:posList3","id5:tf5..")>
			int nPosts = 0;
			Map<String,Double> map = new TreeMap<String, Double>();
			for (Text value : values) {// value="id1:tf1"
				String[] arr = value.toString().split(":");
				String id = arr[0];
				String posList = arr[2];
				String idposList = id + ":" + posList;
				double tf = Double.parseDouble(arr[1]);
				map.put(idposList, tf);
				nPosts++;
			}

			//sort by tf
		    List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String,Double>>(map.entrySet());
		    Collections.sort(list, valueComparator);
			
			int numDoc = Integer.parseInt(context.getConfiguration().get("D"));
			double idf = Math.log( numDoc / ( 1d + (double) nPosts ) );
		    
			JSONArray valueArray = new JSONArray();
			for (Map.Entry<String, Double> entry : list) {
				JSONObject idScoreJSON = new JSONObject();
				try {
					String[] idposList = entry.getKey().toString().split(":");
					idScoreJSON.put("id", Integer.parseInt(idposList[0]));
					idScoreJSON.put("tfidf", entry.getValue() * idf);
					
					String[] positions = idposList[1].split(",");
					JSONArray posArray = new JSONArray();
					for(String pos: positions)
						posArray.put(Integer.parseInt(pos));
					idScoreJSON.put("positions", posArray);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				valueArray.put(idScoreJSON);
		    }
			
			JSONObject index = new JSONObject();
			try {
				index.put(key.toString(), valueArray);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			result.set(index.toString());
			context.write(result, new Text());
			
		}
	}
	
	static Comparator<Map.Entry<String, Double>> valueComparator = new Comparator<Map.Entry<String,Double>>() {
        @Override
        public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
            // TODO Auto-generated method stub
            return (o1.getValue() < o2.getValue())? 1:-1;
        }
    };

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "inverted index");
		job.setJarByClass(InvertedIndex.class);
		job.setMapperClass(IndexMapper.class);
		job.setCombinerClass(IndexCombiner.class);
		job.setReducerClass(IndexReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.getConfiguration().set("D", args[2]);
		
		long startTime = System.currentTimeMillis();
		FileWriter fw = new FileWriter("time.txt");
		job.waitForCompletion(true);
		fw.write((System.currentTimeMillis() - startTime)/1000d + "s");
		fw.close();
		//System.out.println("Using Time: " + (System.currentTimeMillis() - startTime)/1000d + "s");
		
		//System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
