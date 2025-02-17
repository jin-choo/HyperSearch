package algorithms.HyperSearch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;


public class Main {
	public static void main(String[] arg) throws IOException {
		ArrayList<int[]> relaxationRatios = new ArrayList<>() {{
			add(new int[]{3, 3, 3});
			add(new int[]{3, 3, 4});
			add(new int[]{3, 3, 5});
			add(new int[]{3, 4, 3});
			add(new int[]{3, 4, 4});
			add(new int[]{3, 4, 5});
			add(new int[]{3, 5, 3});
			add(new int[]{3, 5, 4});
			add(new int[]{3, 5, 5});
			add(new int[]{4, 3, 3});
			add(new int[]{4, 3, 4});
			add(new int[]{4, 3, 5});
			add(new int[]{4, 4, 3});
			add(new int[]{4, 4, 4});
			add(new int[]{4, 4, 5});
			add(new int[]{4, 5, 3});
			add(new int[]{4, 5, 4});
			add(new int[]{4, 5, 5});
			add(new int[]{5, 3, 3});
			add(new int[]{5, 3, 4});
			add(new int[]{5, 3, 5});
			add(new int[]{5, 4, 3});
			add(new int[]{5, 4, 4});
			add(new int[]{5, 4, 5});
			add(new int[]{5, 5, 3});
			add(new int[]{5, 5, 4});
			add(new int[]{5, 5, 5});
		}};
		ArrayList<Integer> numPredictions = new ArrayList<>() {{
			add(1);
			add(2);
			add(5);
//			add(4);
//			add(10);
		}};
		ArrayList<Double> weights = new ArrayList<>() {{
			add(0.0);
			add(0.1);
			add(1.0);
			add(10.0);
		}};
		
		double recall;
		double avgF1;
		double avgTime;
		double recallMax;
		double avgF1Max;
		double avgTimeMax;
		List<Double> recallList = new ArrayList<>();
		List<Double> avgF1List = new ArrayList<>();
		List<Double> timeList = new ArrayList<>();
		int[] relaxationRatioMax = new int[3];
		double weightMax = 0.0;

		int splits = 5;

		ArrayList<String> noTimeDatasetList = new ArrayList<>() {{
			add("cora");
			add("citeseer");
			add("coraA");
			add("dblp");
			add("dblpA");
			add("pubmed");
		}};
		ArrayList<String> bigDatasetList = new ArrayList<>() {{
			add("dblpA");
			add("email-Eu");
		}};

		File file;
		List<String> lines;
		int linesSize;
		String line;
		int time;
		int minTime;
		int maxTime;
		int[] groundTrainTime;
		int[] groundTime;
		int[] valTrainTime;
		double[] normGroundTrainTime;
		double[] normGroundTime = new double[0];
        double[] decayTime = new double[0];

		String dataset = arg[0];
		boolean timeDataset = !noTimeDatasetList.contains(dataset);
		boolean bigDataset = bigDatasetList.contains(dataset);

		List<Double> precisionRecallF1AvgF1;
		recallMax = 0.0;
		avgF1Max = 0.0;
		avgTimeMax = 0.0;

		System.out.printf("%s: valid%n", dataset);
		System.out.println();

		Files.createDirectories(Paths.get("./result"));
		Files.createDirectories(Paths.get("./time"));

		long startTimestamp = System.currentTimeMillis();

		if (timeDataset) {
			file = new File(String.format("../data/ground_train_time/%s.txt", dataset));
			lines = Files.readAllLines(file.toPath());
			linesSize = lines.size();
			groundTrainTime = new int[linesSize];
			minTime = Integer.MAX_VALUE;
			maxTime = 0;
			for (int i = 0; i < linesSize; i++) {
				line = lines.get(i);
				if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
					continue;
				}
				time = Integer.parseInt(line);
				groundTrainTime[i] = time;
				if (time < minTime) {
					minTime = time;
				}
				if (time > maxTime) {
					maxTime = time;
				}
			}
			normGroundTrainTime = new double[linesSize];
			for (int i = 0; i < linesSize; i++) {
				time = groundTrainTime[i];
				normGroundTrainTime[i] = (double) (time - minTime) / (double) (maxTime - minTime);
			}

			file = new File(String.format("../data/ground_time/%s.txt", dataset));
			lines = Files.readAllLines(file.toPath());
			linesSize = lines.size();
			groundTime = new int[linesSize];
			minTime = Integer.MAX_VALUE;
			maxTime = 0;
			for (int i = 0; i < linesSize; i++) {
				line = lines.get(i);
				if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
					continue;
				}
				time = Integer.parseInt(line);
				groundTime[i] = time;
				if (time < minTime) {
					minTime = time;
				}
				if (time > maxTime) {
					maxTime = time;
				}
			}
			normGroundTime = new double[linesSize];
			for (int i = 0; i < linesSize; i++) {
				time = groundTime[i];
				normGroundTime[i] = (double) (time - minTime) / (double) (maxTime - minTime);
			}

			file = new File(String.format("../data/val_train_time/%s.txt", dataset));
			lines = Files.readAllLines(file.toPath());
			linesSize = lines.size();
			valTrainTime = new int[linesSize];
			minTime = Integer.MAX_VALUE;
			maxTime = 0;
			for (int i = 0; i < linesSize; i++) {
				line = lines.get(i);
				if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
					continue;
				}
				time = Integer.parseInt(line);
				valTrainTime[i] = time;
				if (time < minTime) {
					minTime = time;
				}
				if (time > maxTime) {
					maxTime = time;
				}
			}
            double[] normValTrainTime = new double[linesSize];
            for (int i = 0; i < linesSize; i++) {
				time = valTrainTime[i];
				normValTrainTime[i] = (double) (time - minTime) / (double) (maxTime - minTime);
			}

			for (double weight : weights) {
				if (weight != 0.0) {
					decayTime = new double[normGroundTrainTime.length];
					if (weight > 0.0) {
						for (int i = 0; i < normGroundTrainTime.length; i++) {
							decayTime[i] = Math.exp(weight*normGroundTrainTime[i]);
						}
					} else {
						for (int i = 0; i < normGroundTrainTime.length; i++) {
							decayTime[i] = Math.exp(weight*(normGroundTrainTime[i]-1));
						}
					}
				}
				for (int[] relaxationRatio : relaxationRatios) {
					HyperSearch_time algo = new HyperSearch_time();
					precisionRecallF1AvgF1 = algo.runAlgorithm(dataset, 1, relaxationRatio[0], relaxationRatio[1], relaxationRatio[2], weight, decayTime, "valid", bigDataset);
					recall = precisionRecallF1AvgF1.get(1);
					avgF1 = precisionRecallF1AvgF1.get(3);
					avgTime = precisionRecallF1AvgF1.get(4);
					System.out.printf("%d, %d, %d, %.1f: %.4f, %.4f, %.0f%n", relaxationRatio[0], relaxationRatio[1], relaxationRatio[2], weight, recall, avgF1, avgTime);
					if (recallMax < recall || (recallMax == recall && avgF1Max < avgF1)) {
						recallMax = recall;
						avgF1Max = avgF1;
						avgTimeMax = avgTime;
						relaxationRatioMax[0] = relaxationRatio[0];
						relaxationRatioMax[1] = relaxationRatio[1];
						relaxationRatioMax[2] = relaxationRatio[2];
						weightMax = weight;
					}
				}
			}
		} else {
			for (int[] relaxationRatio : relaxationRatios) {
				for (double weight : weights) {
					recallList = new ArrayList<>();
					avgF1List = new ArrayList<>();
					timeList = new ArrayList<>();
					for (int split = 0; split < splits; split++) {
						HyperSearch_nodefeat algo = new HyperSearch_nodefeat();
						precisionRecallF1AvgF1 = algo.runAlgorithm(dataset, 1, relaxationRatio[0], relaxationRatio[1], relaxationRatio[2], weight, split, "valid", bigDataset);
						recallList.add(precisionRecallF1AvgF1.get(1));
						avgF1List.add(precisionRecallF1AvgF1.get(3));
						timeList.add(precisionRecallF1AvgF1.get(4));
					}
					recall = recallList.stream()
							.mapToDouble(e -> e / (double) splits).sum();
					avgF1 = avgF1List.stream()
							.mapToDouble(e -> e / (double) splits).sum();
					avgTime = timeList.stream()
							.mapToDouble(e -> e / (double) splits).sum();
					System.out.printf("%d, %d, %d, %.1f: %.4f, %.4f, %.0f%n", relaxationRatio[0], relaxationRatio[1], relaxationRatio[2], weight, recall, avgF1, avgTime);
					if (recallMax < recall || (recallMax == recall && avgF1Max < avgF1)) {
						recallMax = recall;
						avgF1Max = avgF1;
						avgTimeMax = avgTime;
						relaxationRatioMax[0] = relaxationRatio[0];
						relaxationRatioMax[1] = relaxationRatio[1];
						relaxationRatioMax[2] = relaxationRatio[2];
						weightMax = weight;
					}
				}
			}
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("./result/%s_result_%s.csv", dataset, new SimpleDateFormat("MM.dd_HH.mm").format(startTimestamp))));
		writer.write("dataset,#outcomes,faultE,faultV,faultP,constant,validTest,recall,avgf1,time\n");
		writer.write(String.format("%s,1,%d,%d,%d,%.1f,valid,%f,%f,%f\n", dataset, relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, recallMax, avgF1Max, avgTimeMax));

		System.out.println();
		if (timeDataset) {
			System.out.printf("%d, %d, %d, %.1f: %.4f, %.4f, %.0f%n", relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, recallMax, avgF1Max, avgTimeMax);
		} else {
			System.out.printf("%d, %d, %d, %.1f: %.4f, %.4f, %.0f%n", relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, recallMax, avgF1Max, avgTimeMax);
		}
		System.out.println();

		for (Integer numPrediction : numPredictions) {
			System.out.printf("%s: multiple %d%n", dataset, numPrediction);
			System.out.println();

			if (timeDataset) {
				if (weightMax != 0.0) {
					decayTime = new double[normGroundTime.length];
					if (weightMax > 0.0) {
						for (int i = 0; i < normGroundTime.length; i++) {
							decayTime[i] = Math.exp(weightMax * normGroundTime[i]);
						}
					} else {
						for (int i = 0; i < normGroundTime.length; i++) {
							decayTime[i] = Math.exp(weightMax * (normGroundTime[i] - 1));
						}
					}
				}
				HyperSearch_time algo = new HyperSearch_time();
				precisionRecallF1AvgF1 = algo.runAlgorithm(dataset, numPrediction, relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, decayTime, "valid max", bigDataset);
				recall = precisionRecallF1AvgF1.get(1);
				avgF1 = precisionRecallF1AvgF1.get(3);
				avgTime  = precisionRecallF1AvgF1.get(4);
			} else {
				for (int split = 0; split < splits; split++) {
					HyperSearch_nodefeat algo = new HyperSearch_nodefeat();
					precisionRecallF1AvgF1 = algo.runAlgorithm(dataset, numPrediction, relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, split, "valid max", bigDataset);
					recallList.set(split, precisionRecallF1AvgF1.get(1));
					avgF1List.set(split, precisionRecallF1AvgF1.get(3));
					timeList.set(split, precisionRecallF1AvgF1.get(4));
				}
				recall = recallList.stream()
						.mapToDouble(e -> e / (double) splits).sum();
				avgF1 = avgF1List.stream()
						.mapToDouble(e -> e / (double) splits).sum();
				avgTime = timeList.stream()
						.mapToDouble(e -> e / (double) splits).sum();
			}
			System.out.printf("valid: %.3f, %.3f, %.0f%n", recall, avgF1, avgTime);
			System.out.println();

			if (timeDataset) {
				if (weightMax != 0.0) {
					decayTime = new double[normGroundTime.length];
					if (weightMax > 0.0) {
						for (int i = 0; i < normGroundTime.length; i++) {
							decayTime[i] = Math.exp(weightMax * normGroundTime[i]);
						}
					} else {
						for (int i = 0; i < normGroundTime.length; i++) {
							decayTime[i] = Math.exp(weightMax * (normGroundTime[i] - 1));
						}
					}
				}
				HyperSearch_time algo = new HyperSearch_time();
				precisionRecallF1AvgF1 = algo.runAlgorithm(dataset, numPrediction, relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, decayTime, "test", bigDataset);
				recall = precisionRecallF1AvgF1.get(1);
				avgF1 = precisionRecallF1AvgF1.get(3);
				avgTime = precisionRecallF1AvgF1.get(4);
			} else {
				for (int split = 0; split < splits; split++) {
					HyperSearch_nodefeat algo = new HyperSearch_nodefeat();
					precisionRecallF1AvgF1 = algo.runAlgorithm(dataset, numPrediction, relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, split, "test", bigDataset);
					recallList.set(split, precisionRecallF1AvgF1.get(1));
					avgF1List.set(split, precisionRecallF1AvgF1.get(3));
					timeList.set(split, precisionRecallF1AvgF1.get(4));
				}
				recall = recallList.stream()
						.mapToDouble(e -> e / (double) splits).sum();
				avgF1 = avgF1List.stream()
						.mapToDouble(e -> e / (double) splits).sum();
				avgTime = timeList.stream()
						.mapToDouble(e -> e / (double) splits).sum();
			}
			
			writer.write(String.format("%s,%d,%d,%d,%d,%.1f,test,%f,%f,%f\n", dataset, numPrediction, relaxationRatioMax[0], relaxationRatioMax[1], relaxationRatioMax[2], weightMax, recall, avgF1, avgTime));

			System.out.printf("test: %.3f, %.3f, %.1f%n", recall, avgF1, avgTime);
			System.out.println();
		}
		writer.close();
	}
}