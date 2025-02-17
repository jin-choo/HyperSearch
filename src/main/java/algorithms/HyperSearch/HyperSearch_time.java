package algorithms.HyperSearch;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HyperSearch_time {
	int faultE;
	int faultV;
	int faultP;
	int epsilonE;

	boolean bigDataset;
	double growth;
	double[] decayTime;

	int hashedNumber = 0;
	List<List<Integer>> hashedList = new ArrayList<>();
	List<Map<List<Integer>, Integer>> hashedMap = new ArrayList<>();

	Set<Integer> ground2 = new HashSet<>();
	List<Set<List<Integer>>> groundSize;
	List<Short> groundLen;
	Map<Integer, Set<Integer>> nodeEdgeSetGround;
	Map<Short, Integer> hyperedgeSizeDistGround;

	List<Integer> testData;
	List<List<Integer>> testDataSize;
	Set<Integer> uniqueTestData;
	int lenTestData;

	Map<Short, Integer> hyperedgeSizeCount;

	int nv;

	int maxLenHyperedge = 0;

	Map<Double, Set<Integer>> HOISupportNoSize = new HashMap<>();
	List<Map<Double, Set<Integer>>> HOISupport = new ArrayList<>();
	int HOICountNoSize = 0;
	int[] HOICount;
	double HOIMinsupNoSize = 0;
	double[] HOIMinsup;

	boolean degreeReversed = true;

	public HyperSearch_time() {

	}

	public List<Double> runAlgorithm(String data, int numOutcomes, int faultE, int faultV, int faultP, double growth, double[] decayTime, String validTest, boolean bigDataset) throws IOException {
		long startTimestamp = System.currentTimeMillis();

		this.faultE = faultE;
		this.faultV = faultV;
		this.faultP = faultP;

		this.bigDataset = bigDataset;
		this.growth = growth;
		this.decayTime = decayTime;

		BufferedReader reader = new BufferedReader(new FileReader(String.format("../data/nv/%s.txt", data)));
		this.nv = Integer.parseInt(reader.readLine());
		reader.close();

		groundSize = new ArrayList<>();
		groundLen = new ArrayList<>();
		nodeEdgeSetGround = new LinkedHashMap<>();
		int hyperedgeCount = 0;

		if (validTest.startsWith("valid")) {
			reader = new BufferedReader(new FileReader(String.format("../data/ground_train_data/%s.txt", data)));
		} else if (validTest.equals("test")) {
			reader = new BufferedReader(new FileReader(String.format("../data/ground_data/%s.txt", data)));
		} else if (validTest.equals("val")) {
			reader = new BufferedReader(new FileReader(String.format("../data/val_train/%s.txt", data)));
		}

		String line;
		String[] lineSplited;
		int lineSplitedLength;
		int item1;
		int item2;
		List<Integer> hyperedge;
		while (((line = reader.readLine()) != null)) {
			if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}
			lineSplited = line.split(" ");
			lineSplitedLength = lineSplited.length;
			if (lineSplitedLength > maxLenHyperedge) {
				maxLenHyperedge = lineSplitedLength;
				if (groundSize.size() < maxLenHyperedge-2) {
					for (int i = groundSize.size(); i < maxLenHyperedge-2; i++) {
						groundSize.add(new HashSet<>());
						hashedMap.add(new HashMap<>());
					}
				}
			}
			if (lineSplitedLength == 2) {
				item1 = Integer.parseInt(lineSplited[0]);
				item2 = Integer.parseInt(lineSplited[1]);
				if (!nodeEdgeSetGround.containsKey(item1)) {
					nodeEdgeSetGround.put(item1, new HashSet<>());
				}
				nodeEdgeSetGround.get(item1).add(hyperedgeCount);
				if (!nodeEdgeSetGround.containsKey(item2)) {
					nodeEdgeSetGround.put(item2, new HashSet<>());
				}
				nodeEdgeSetGround.get(item2).add(hyperedgeCount);
				if (item1 < item2) {
					ground2.add(item1 * nv + item2);
				} else {
					ground2.add(item2 * nv + item1);
				}
			} else {
				hyperedge = new ArrayList<>();
				for (int i = 0; i < lineSplitedLength; i++) {
					item1 = Integer.parseInt(lineSplited[i]);
					hyperedge.add(item1);
					if (!nodeEdgeSetGround.containsKey(item1)) {
						nodeEdgeSetGround.put(item1, new HashSet<>());
					}
					nodeEdgeSetGround.get(item1).add(hyperedgeCount);
				}
				Collections.sort(hyperedge);
				hyperedge = Collections.unmodifiableList(hyperedge);
				groundSize.get(lineSplitedLength-3).add(hyperedge);
			}
			groundLen.add((short) lineSplitedLength);
			hyperedgeCount++;
		}
		reader.close();

		Comparator<Entry<Integer, Set<Integer>>> sortNodeEdgeSetGround1 = Comparator.comparingInt(e -> e.getValue().size());
		Comparator<Entry<Integer, Set<Integer>>> sortNodeEdgeSetGround2 = Comparator.comparingInt(Entry::getKey);
		nodeEdgeSetGround = nodeEdgeSetGround.entrySet().stream()
				.sorted(sortNodeEdgeSetGround1.thenComparing(sortNodeEdgeSetGround2))
				.collect(Collectors.toMap(
						Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new));

		hyperedgeSizeDistGround = new HashMap<>();
		int groundSizeGetSize;
		short lineSplitedLengthShort;
		int totalLineSplitedLength = 0;

		if (validTest.equals("val")) {
			groundSizeGetSize = ground2.size();
			hyperedgeSizeDistGround.put((short) (2), groundSizeGetSize);
			totalLineSplitedLength += groundSizeGetSize;
			for (int i = 0; i < groundSize.size(); i++) {
				groundSizeGetSize = groundSize.get(i).size();
				hyperedgeSizeDistGround.put((short) (i+3), groundSizeGetSize);
				totalLineSplitedLength += groundSizeGetSize;
			}
		} else {
			if (validTest.startsWith("valid")) {
				reader = new BufferedReader(new FileReader(String.format("../data/unique_ground_train_data/%s.txt", data)));
			} else if (validTest.equals("test")) {
				reader = new BufferedReader(new FileReader(String.format("../data/unique_ground_data/%s.txt", data)));
			}
			while (((line = reader.readLine()) != null)) {
				if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
					continue;
				}
				lineSplitedLengthShort = (short) line.split(" ").length;
				if (lineSplitedLengthShort != (short) 1) {
					if (!hyperedgeSizeDistGround.containsKey(lineSplitedLengthShort)) {
						hyperedgeSizeDistGround.put(lineSplitedLengthShort, 0);
					}
					hyperedgeSizeDistGround.put(lineSplitedLengthShort, hyperedgeSizeDistGround.get(lineSplitedLengthShort)+1);
					totalLineSplitedLength += 1;
				}
			}
			reader.close();
		}
		Short hyperedgeSize;
		Integer hyperedgeSizeDist;
		List<Short> removedHyperedgeSize = new ArrayList<>();
		for (Entry<Short, Integer> entry : hyperedgeSizeDistGround.entrySet()) {
			hyperedgeSize = entry.getKey();
			hyperedgeSizeDist = entry.getValue();
			if (!bigDataset) {
				if ((hyperedgeSize > (short) 5 && (double) hyperedgeSizeDist / (double) totalLineSplitedLength < 0.01) || hyperedgeSize > 10) {
					removedHyperedgeSize.add(hyperedgeSize);
				}
			} else {
				if (hyperedgeSize > 5) {
					removedHyperedgeSize.add(hyperedgeSize);
				}
			}
		}
		for (Short removedHESize : removedHyperedgeSize) {
			totalLineSplitedLength -= hyperedgeSizeDistGround.get(removedHESize);
			hyperedgeSizeDistGround.remove(removedHESize);
		}

		Map<Short, Double> hyperedgeSizeDistDoubleGround = new HashMap<>();
		Short maxHyperedgeSize = (short) 0;
		for (Entry<Short, Integer> entry : hyperedgeSizeDistGround.entrySet()) {
			hyperedgeSize = entry.getKey();
			hyperedgeSizeDist = entry.getValue();
			hyperedgeSizeDistDoubleGround.put(hyperedgeSize, (double) hyperedgeSizeDist / (double) totalLineSplitedLength);
			if (hyperedgeSize > maxHyperedgeSize) {
				maxHyperedgeSize = hyperedgeSize;
			}
		}

		testData = new ArrayList<>();
		testDataSize = new ArrayList<>();
		for (int i = 0; i < maxHyperedgeSize-1; i++) {
			testDataSize.add(new ArrayList<>());
		}
		uniqueTestData = new HashSet<>();
		int uniqueTestDataCount = 0;
		if (validTest.startsWith("valid")) {
			reader = new BufferedReader(new FileReader(String.format("../data/ground_valid_data/%s.txt", data)));
		} else if (validTest.equals("test")) {
			reader = new BufferedReader(new FileReader(String.format("../data/test_data/%s.txt", data)));
		} else if (validTest.equals("val")) {
			reader = new BufferedReader(new FileReader(String.format("../data/val_test/%s.txt", data)));
		}
		Integer hyperedgeNumber;
		while (((line = reader.readLine()) != null)) {
			if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
				continue;
			}
			lineSplited = line.split(" ");
			lineSplitedLength = lineSplited.length;
			if (hyperedgeSizeDistDoubleGround.containsKey((short) lineSplitedLength)) {
				if (lineSplitedLength == 2) {
					item1 = Integer.parseInt(lineSplited[0]);
					item2 = Integer.parseInt(lineSplited[1]);
					if (item1 < item2) {
						hyperedgeNumber = item1 * nv + item2;
					} else {
						hyperedgeNumber = item2 * nv + item1;
					}
					testData.add(-hyperedgeNumber);
					testDataSize.get(0).add(-hyperedgeNumber);
					if (!uniqueTestData.contains(-hyperedgeNumber)) {
						uniqueTestData.add(-hyperedgeNumber);
						uniqueTestDataCount++;
					}
				} else {
					hyperedge = new ArrayList<>();
					for (int i = 0; i < lineSplitedLength; i++) {
						hyperedge.add(Integer.parseInt(lineSplited[i]));
					}
					Collections.sort(hyperedge);
					hyperedge = Collections.unmodifiableList(hyperedge);
					hyperedgeNumber = hyperedgeToHashed(hyperedge, lineSplitedLength-2);
					testData.add(hyperedgeNumber);
					testDataSize.get(lineSplitedLength-2).add(hyperedgeNumber);
					if (!uniqueTestData.contains(hyperedgeNumber)) {
						uniqueTestData.add(hyperedgeNumber);
						uniqueTestDataCount++;
					}
				}
			}
		}
		reader.close();

		lenTestData = uniqueTestDataCount * numOutcomes;
		hyperedgeSizeCount = new HashMap<>();
		Double hyperedgeSizeDistDouble;
		for (Entry<Short, Double> entry : hyperedgeSizeDistDoubleGround.entrySet()) {
			hyperedgeSize = entry.getKey();
			hyperedgeSizeDistDouble = entry.getValue();
			hyperedgeSizeCount.put(hyperedgeSize, (int) Math.round(hyperedgeSizeDistDouble * lenTestData));
		}

		Loader.loadNativeLibraries();

		processAlgorithm();

		List<Integer> hyperedgeCandidates = new ArrayList<>();
		List<Double> hyperedgeCandidatesSup = new ArrayList<>();
		List<Integer> hyperedgeCandidatesSize;
		int lenHyperedgeCandidates = 0;
		int lenHyperedgeCandidatesSize;
		Integer hyperedgeSizeCountGet;
		Comparator<Entry<Double, Set<Integer>>> sortMapByKey = Comparator.comparingDouble(Entry::getKey);
		Map<Double, Set<Integer>> HOIDegree;
		double degree;
		int numHyperedgeCount;
		Map<Double, Set<Integer>> supportHOICombSet;
		double HOISizeSupport;
		Set<Integer> HOISizeSet;
		Map<List<Integer>, Integer> hashedMapGet;
		Integer HOICombNumber;

		hyperedgeCandidatesSize = new ArrayList<>();
		lenHyperedgeCandidatesSize = 0;
		hyperedgeSizeCountGet = hyperedgeSizeCount.get((short) (2));
		for (Entry<Double, Set<Integer>> entry : HOISupport.get(0).entrySet().stream()
				.sorted(sortMapByKey.reversed())
				.collect(Collectors.toMap(
						Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).entrySet()) {
			Double HOISetSup = entry.getKey();
			Set<Integer> HOISet = entry.getValue();
			if (lenHyperedgeCandidatesSize + HOISet.size() > hyperedgeSizeCountGet) {
				HOIDegree = new HashMap<>();
				for (Integer HOINumber : HOISet) {
					degree = nodeEdgeSetGround.get(HOINumber / nv).size() + nodeEdgeSetGround.get(HOINumber % nv).size();
					if (!HOIDegree.containsKey(degree)) {
						HOIDegree.put(degree, new HashSet<>());
					}
					HOIDegree.get(degree).add(HOINumber);
				}
				if (degreeReversed) {
					for (Set<Integer> HOIDegreeSet : HOIDegree.entrySet().stream()
							.sorted(sortMapByKey.reversed())
							.collect(Collectors.toMap(
									Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).values()) {
						for (Integer HOINumber : HOIDegreeSet) {
							hyperedgeCandidates.add(-HOINumber);
							hyperedgeCandidatesSup.add(HOISetSup);
							hyperedgeCandidatesSize.add(-HOINumber);
							lenHyperedgeCandidates++;
							lenHyperedgeCandidatesSize++;
						}
						if (lenHyperedgeCandidatesSize >= hyperedgeSizeCountGet) {
							break;
						}
					}
				} else {
					for (Set<Integer> HOIDegreeSet : HOIDegree.entrySet().stream()
							.sorted(sortMapByKey)
							.collect(Collectors.toMap(
									Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).values()) {
						for (Integer HOINumber : HOIDegreeSet) {
							hyperedgeCandidates.add(-HOINumber);
							hyperedgeCandidatesSup.add(HOISetSup);
							hyperedgeCandidatesSize.add(-HOINumber);
							lenHyperedgeCandidates++;
							lenHyperedgeCandidatesSize++;
						}
						if (lenHyperedgeCandidatesSize >= hyperedgeSizeCountGet) {
							break;
						}
					}
				}
				break;
			}
			for (Integer HOINumber : HOISet) {
				hyperedgeCandidates.add(-HOINumber);
				hyperedgeCandidatesSup.add(HOISetSup);
				hyperedgeCandidatesSize.add(-HOINumber);
				lenHyperedgeCandidates++;
				lenHyperedgeCandidatesSize++;
			}
		}
		List<Double> precisionRecallF1AvgF1Size = computePrecisionRecallF1(testDataSize.get(0), hyperedgeCandidatesSize);
		precisionRecallF1AvgF1Size.add(computeAvgF1Size(testData, testDataSize.get(0), hyperedgeCandidates));
		if (!validTest.equals("valid")) {
			System.out.printf("%d: %d / %d, %.3f, %.3f%n", 2, lenHyperedgeCandidatesSize, hyperedgeSizeCountGet, precisionRecallF1AvgF1Size.get(1), precisionRecallF1AvgF1Size.get(3));
		}
		if (lenHyperedgeCandidatesSize < hyperedgeSizeCountGet) {
			numHyperedgeCount = hyperedgeSizeCountGet - lenHyperedgeCandidatesSize;
			supportHOICombSet = new HashMap<>();
			for (int HOISupportGetSize = 1; HOISupportGetSize < HOISupport.size(); HOISupportGetSize++) {
				for (Entry<Double, Set<Integer>> entry : HOISupport.get(HOISupportGetSize).entrySet()) {
					HOISizeSupport = entry.getKey();
					HOISizeSet = entry.getValue();
					for (Integer HOINumber : HOISizeSet) {
						for (List<Integer> HOIComb : generateCombinations(hashedList.get(HOINumber), 2)) {
							if (HOIComb.get(0) < HOIComb.get(1)) {
								HOICombNumber = HOIComb.get(0) * nv + HOIComb.get(1);
							} else {
								HOICombNumber = HOIComb.get(1) * nv + HOIComb.get(0);
							}
							if (!ground2.contains(HOICombNumber) && !hyperedgeCandidatesSize.contains(-HOICombNumber)) {
								if (!supportHOICombSet.containsKey(HOISizeSupport)) {
									supportHOICombSet.put(HOISizeSupport, new HashSet<>());
								}
								supportHOICombSet.get(HOISizeSupport).add(HOICombNumber);
							}
						}
					}
				}
			}
			for (Entry<Double, Set<Integer>> entry : supportHOICombSet.entrySet().stream()
					.sorted(sortMapByKey.reversed())
					.collect(Collectors.toMap(
							Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).entrySet()) {
				Double HOISetSup = entry.getKey();
				Set<Integer> HOICombSet = entry.getValue();
				for (Integer HOIComb : HOICombSet) {
					hyperedgeCandidates.add(-HOIComb);
					hyperedgeCandidatesSup.add(HOISetSup);
					hyperedgeCandidatesSize.add(-HOIComb);
					lenHyperedgeCandidates++;
					lenHyperedgeCandidatesSize++;
				}
				if (lenHyperedgeCandidatesSize >= hyperedgeSizeCountGet) {
					break;
				}
			}
			if (!validTest.equals("valid")) {
				System.out.printf("Size %d: %d -> %d%n", 2, numHyperedgeCount, hyperedgeSizeCountGet - lenHyperedgeCandidatesSize);
			}
		}

		for (int HOISize = 1; HOISize < HOISupport.size(); HOISize++) {
			hyperedgeCandidatesSize = new ArrayList<>();
			lenHyperedgeCandidatesSize = 0;
			hyperedgeSizeCountGet = hyperedgeSizeCount.get((short) (HOISize+2));
			for (Entry<Double, Set<Integer>> entry : HOISupport.get(HOISize).entrySet().stream()
					.sorted(sortMapByKey.reversed())
					.collect(Collectors.toMap(
							Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).entrySet()) {
				Double HOISetSup = entry.getKey();
				Set<Integer> HOISet = entry.getValue();
				if (lenHyperedgeCandidatesSize + HOISet.size() > hyperedgeSizeCountGet) {
					HOIDegree = new HashMap<>();
					for (Integer HOINumber : HOISet) {
						degree = 0.0;
						for (Integer HOInode : hashedList.get(HOINumber)) {
							degree += nodeEdgeSetGround.get(HOInode).size();
						}
						if (!HOIDegree.containsKey(degree)) {
							HOIDegree.put(degree, new HashSet<>());
						}
						HOIDegree.get(degree).add(HOINumber);
					}
					if (degreeReversed) {
						for (Set<Integer> HOIDegreeSet : HOIDegree.entrySet().stream()
								.sorted(sortMapByKey.reversed())
								.collect(Collectors.toMap(
										Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).values()) {
							for (Integer HOINumber : HOIDegreeSet) {
								hyperedgeCandidates.add(HOINumber);
								hyperedgeCandidatesSup.add(HOISetSup);
								hyperedgeCandidatesSize.add(HOINumber);
								lenHyperedgeCandidates++;
								lenHyperedgeCandidatesSize++;
							}
							if (lenHyperedgeCandidatesSize >= hyperedgeSizeCountGet) {
								break;
							}
						}
					} else {
						for (Set<Integer> HOIDegreeSet : HOIDegree.entrySet().stream()
								.sorted(sortMapByKey)
								.collect(Collectors.toMap(
										Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).values()) {
							for (Integer HOINumber : HOIDegreeSet) {
								hyperedgeCandidates.add(HOINumber);
								hyperedgeCandidatesSup.add(HOISetSup);
								hyperedgeCandidatesSize.add(HOINumber);
								lenHyperedgeCandidates++;
								lenHyperedgeCandidatesSize++;
							}
							if (lenHyperedgeCandidatesSize >= hyperedgeSizeCountGet) {
								break;
							}
						}
					}
					break;
				}
				for (Integer HOINumber : HOISet) {
					hyperedgeCandidates.add(HOINumber);
					hyperedgeCandidatesSup.add(HOISetSup);
					hyperedgeCandidatesSize.add(HOINumber);
					lenHyperedgeCandidates++;
					lenHyperedgeCandidatesSize++;
				}
			}
			precisionRecallF1AvgF1Size = computePrecisionRecallF1(testDataSize.get(HOISize), hyperedgeCandidatesSize);
			precisionRecallF1AvgF1Size.add(computeAvgF1Size(testData, testDataSize.get(HOISize), hyperedgeCandidates));
			if (!validTest.equals("valid")) {
				System.out.printf("%d: %d / %d, %.3f, %.3f%n", HOISize+2, lenHyperedgeCandidatesSize, hyperedgeSizeCountGet, precisionRecallF1AvgF1Size.get(1), precisionRecallF1AvgF1Size.get(3));
			}
			if (lenHyperedgeCandidatesSize < hyperedgeSizeCountGet) {
				numHyperedgeCount = hyperedgeSizeCountGet - lenHyperedgeCandidatesSize;
				supportHOICombSet = new HashMap<>();
				for (int HOISupportGetSize = 1; HOISupportGetSize < HOISupport.size(); HOISupportGetSize++) {
					for (Entry<Double, Set<Integer>> entry : HOISupport.get(HOISupportGetSize).entrySet()) {
						HOISizeSupport = entry.getKey();
						HOISizeSet = entry.getValue();
						for (Integer HOINumber : HOISizeSet) {
							for (List<Integer> HOIComb : generateCombinations(hashedList.get(HOINumber), HOISize+2)) {
								Collections.sort(HOIComb);
								HOIComb = Collections.unmodifiableList(HOIComb);
								if (!groundSize.get(HOISize-1).contains(HOIComb)) {
									hashedMapGet = hashedMap.get(HOISize-1);
									if (!hashedMapGet.containsKey(HOIComb)) {
										hashedList.add(HOIComb);
										hashedMapGet.put(HOIComb, hashedNumber);
										HOICombNumber = hashedNumber;
										hashedNumber++;
										if (!supportHOICombSet.containsKey(HOISizeSupport)) {
											supportHOICombSet.put(HOISizeSupport, new HashSet<>());
										}
										supportHOICombSet.get(HOISizeSupport).add(HOICombNumber);
									} else {
										HOICombNumber = hashedMapGet.get(HOIComb);
										if (!hyperedgeCandidatesSize.contains(HOICombNumber)) {
											if (!supportHOICombSet.containsKey(HOISizeSupport)) {
												supportHOICombSet.put(HOISizeSupport, new HashSet<>());
											}
											supportHOICombSet.get(HOISizeSupport).add(HOICombNumber);
										}
									}
								}
							}
						}
					}
				}
				for (Entry<Double, Set<Integer>> entry : supportHOICombSet.entrySet().stream()
						.sorted(sortMapByKey.reversed())
						.collect(Collectors.toMap(
								Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).entrySet()) {
					Double HOISetSup = entry.getKey();
					Set<Integer> HOICombSet = entry.getValue();
					for (Integer HOIComb : HOICombSet) {
						hyperedgeCandidates.add(HOIComb);
						hyperedgeCandidatesSup.add(HOISetSup);
						hyperedgeCandidatesSize.add(HOIComb);
						lenHyperedgeCandidates++;
						lenHyperedgeCandidatesSize++;
					}
					if (lenHyperedgeCandidatesSize >= hyperedgeSizeCountGet) {
						break;
					}
				}
				if (!validTest.equals("valid")) {
					System.out.printf("Size %d: %d -> %d%n", HOISize+2, numHyperedgeCount, hyperedgeSizeCountGet - lenHyperedgeCandidatesSize);
				}
			}
		}

		if (lenHyperedgeCandidates < lenTestData) {
			for (Entry<Double, Set<Integer>> entry : HOISupportNoSize.entrySet().stream()
					.sorted(sortMapByKey.reversed())
					.collect(Collectors.toMap(
							Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).entrySet()) {
				Double HOISetSup = entry.getKey();
				Set<Integer> HOISet = entry.getValue();
				hyperedgeCandidates.forEach(HOISet::remove);
				if ((lenHyperedgeCandidates+HOISet.size()) > lenTestData) {
					HOIDegree = new HashMap<>();
					for (Integer HOINumber : HOISet) {
						degree = 0.0;
						for (Integer HOInode : hashedList.get(HOINumber)) {
							degree += nodeEdgeSetGround.get(HOInode).size();
						}
						if (!HOIDegree.containsKey(degree)) {
							HOIDegree.put(degree, new HashSet<>());
						}
						HOIDegree.get(degree).add(HOINumber);
					}
					if (degreeReversed) {
						for (Set<Integer> HOIDegreeSet : HOIDegree.entrySet().stream()
								.sorted(sortMapByKey.reversed())
								.collect(Collectors.toMap(
										Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).values()) {
							for (Integer HOINumber : HOIDegreeSet) {
								hyperedgeCandidates.add(HOINumber);
								hyperedgeCandidatesSup.add(HOISetSup);
								lenHyperedgeCandidates++;
							}
							if (lenHyperedgeCandidates >= lenTestData) {
								break;
							}
						}
					} else {
						for (Set<Integer> HOIDegreeSet : HOIDegree.entrySet().stream()
								.sorted(sortMapByKey)
								.collect(Collectors.toMap(
										Entry::getKey, Entry::getValue, (a, b) -> a, LinkedHashMap::new)).values()) {
							for (Integer HOINumber : HOIDegreeSet) {
								hyperedgeCandidates.add(HOINumber);
								hyperedgeCandidatesSup.add(HOISetSup);
								lenHyperedgeCandidates++;
							}
							if (lenHyperedgeCandidates >= lenTestData) {
								break;
							}
						}
					}
					break;
				}
				for (Integer HOINumber : HOISet) {
					hyperedgeCandidates.add(HOINumber);
					hyperedgeCandidatesSup.add(HOISetSup);
					lenHyperedgeCandidates++;
				}
			}
		}

		long elapsedTimestamp = System.currentTimeMillis() - startTimestamp;

		List<Double> precisionRecallF1AvgF1 = computePrecisionRecallF1(testData, hyperedgeCandidates);
		precisionRecallF1AvgF1.add(computeAvgF1(testData, hyperedgeCandidates));

		if (!validTest.equals("valid")) {
			System.out.printf("total: %d / %d, %.3f, %.3f%n", lenHyperedgeCandidates, lenTestData, precisionRecallF1AvgF1.get(1), precisionRecallF1AvgF1.get(3));
			System.out.println();
		}

		if (validTest.equals("test")) {
			Files.createDirectories(Paths.get("./test_cand"));
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("./test_cand/%s_split_0_%d.txt", data, numOutcomes)));
			for (Integer hyperedgeCandidate : hyperedgeCandidates) {
				if (hyperedgeCandidate > 0) {
					writer.write(hashedList.get(hyperedgeCandidate).toString() + "\n");
				} else if (hyperedgeCandidate < 0) {
					writer.write(String.format("[%d, %d]\n", (-hyperedgeCandidate) / nv, (-hyperedgeCandidate) % nv));
				}
			}
			writer.close();

			Files.createDirectories(Paths.get("./test_cand_sup"));
			writer = new BufferedWriter(new FileWriter(String.format("./test_cand_sup/%s_split_0_%d.txt", data, numOutcomes)));
			for (Double sup : hyperedgeCandidatesSup) {
				writer.write(sup.toString() + "\n");
			}
			writer.close();
		} else if (validTest.equals("val")) {
			Files.createDirectories(Paths.get("./val_cand"));
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("./val_cand/%s_split_0_%d.txt", data, numOutcomes)));
			for (Integer hyperedgeCandidate : hyperedgeCandidates) {
				if (hyperedgeCandidate > 0) {
					writer.write(hashedList.get(hyperedgeCandidate).toString() + "\n");
				} else if (hyperedgeCandidate < 0) {
					writer.write(String.format("[%d, %d]\n", (-hyperedgeCandidate) / nv, (-hyperedgeCandidate) % nv));
				}
			}
			writer.close();

			Files.createDirectories(Paths.get("./val_cand_sup"));
			writer = new BufferedWriter(new FileWriter(String.format("./val_cand_sup/%s_split_0_%d.txt", data, numOutcomes)));
			for (Double sup : hyperedgeCandidatesSup) {
				writer.write(sup.toString() + "\n");
			}
			writer.close();
		} else if (validTest.equals("valid max")) {
			Files.createDirectories(Paths.get("./valid_cand"));
			BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("./valid_cand/%s_split_0_%d.txt", data, numOutcomes)));
			for (Integer hyperedgeCandidate : hyperedgeCandidates) {
				if (hyperedgeCandidate > 0) {
					writer.write(hashedList.get(hyperedgeCandidate).toString() + "\n");
				} else if (hyperedgeCandidate < 0) {
					writer.write(String.format("[%d, %d]\n", (-hyperedgeCandidate) / nv, (-hyperedgeCandidate) % nv));
				}
			}
			writer.close();

			Files.createDirectories(Paths.get("./valid_cand_sup"));
			writer = new BufferedWriter(new FileWriter(String.format("./valid_cand_sup/%s_split_0_%d.txt", data, numOutcomes)));
			for (Double sup : hyperedgeCandidatesSup) {
				writer.write(sup.toString() + "\n");
			}
			writer.close();
		}

		precisionRecallF1AvgF1.add(elapsedTimestamp / 1000.0);

		return precisionRecallF1AvgF1;
	}

	public List<Double> computePrecisionRecallF1(List<Integer> trueHyperedge, List<Integer> predictedHyperedge) {
		int truePositives = 0;
		int totalPositives = 0;
		for (Integer edge : predictedHyperedge) {
			if (trueHyperedge.contains(edge)) {
				truePositives++;
			}
			totalPositives++;
		}
		double precision = 0.0;
		if (totalPositives != 0) {
			precision = (double) truePositives / (double) totalPositives;
		}

		truePositives = 0;
		int actualPositives = 0;
		Set<Integer> predictedHyperedgeSet = new HashSet<>(predictedHyperedge);
		for (Integer edge : trueHyperedge) {
			if (predictedHyperedgeSet.contains(edge)) {
				truePositives++;
			}
			actualPositives++;
		}
		double recall = 0.0;
		if (actualPositives != 0) {
			recall = (double) truePositives / (double) actualPositives;
		}

		double F1 = 0.0;
		if ((precision+recall) != 0.0) {
			F1 = (2.0*precision*recall) / (precision+recall);
		}

		List<Double> precisionRecallF1 = new ArrayList<>();
		precisionRecallF1.add(precision);
		precisionRecallF1.add(recall);
		precisionRecallF1.add(F1);

		return precisionRecallF1;
	}

	public double computeF1(Integer trueEdgeNumber, Integer predictedEdgeNumber) {
		List<Integer> trueEdge = new ArrayList<>();
		if (trueEdgeNumber > 0) {
			trueEdge = hashedList.get(trueEdgeNumber);
		} else if (trueEdgeNumber < 0) {
			trueEdge.add((-trueEdgeNumber) / nv);
			trueEdge.add((-trueEdgeNumber) % nv);
		}
		List<Integer> predictedEdge = new ArrayList<>();
		if (predictedEdgeNumber > 0) {
			predictedEdge = hashedList.get(predictedEdgeNumber);
		} else if (predictedEdgeNumber < 0) {
			predictedEdge.add((-predictedEdgeNumber) / nv);
			predictedEdge.add((-predictedEdgeNumber) % nv);
		}
		int truePositives = 0;
		int totalPositives = 0;
		for (Integer node : predictedEdge) {
			if (trueEdge.contains(node)) {
				truePositives++;
			}
			totalPositives++;
		}
		double precision = 0.0;
		if (totalPositives != 0) {
			precision = (double) truePositives / (double) totalPositives;
		}

		truePositives = 0;
		int actualPositives = 0;
		for (Integer node : trueEdge) {
			if (predictedEdge.contains(node)) {
				truePositives++;
			}
			actualPositives++;
		}
		double recall = 0.0;
		if (actualPositives != 0) {
			recall = (double) truePositives / (double) actualPositives;
		}

		double F1 = 0.0;
		if ((precision+recall) != 0.0) {
			F1 = (2.0*precision*recall) / (precision+recall);
		}

		return F1;
	}

	public double computeAvgF1(List<Integer> trueHyperedge, List<Integer> predictedHyperedge) {
		double avgF1_1 = 0.0;
		double maxF1;
		double F1;

		for (Integer predictedEdge : predictedHyperedge) {
			maxF1 = 0.0;
			for (Integer trueEdge : trueHyperedge) {
				F1 = computeF1(trueEdge, predictedEdge);
				if (F1 > maxF1) {
					maxF1 = F1;
				}
			}
			avgF1_1 += maxF1;
		}

		double avgF1_2 = 0.0;
		for (Integer trueEdge : trueHyperedge) {
			maxF1 = 0.0;
			for (Integer predictedEdge : predictedHyperedge) {
				F1 = computeF1(trueEdge, predictedEdge);
				if (F1 > maxF1) {
					maxF1 = F1;
				}
			}
			avgF1_2 += maxF1;
		}

		return (avgF1_1 / (double) predictedHyperedge.size()+avgF1_2 / (double) trueHyperedge.size()) / 2.0;
	}

	public double computeAvgF1Size(List<Integer> trueHyperedge, List<Integer> trueHyperedgeSize, List<Integer> predictedHyperedge) {
		double avgF1_1 = 0.0;
		double maxF1;
		double F1;

		for (Integer predictedEdge : predictedHyperedge) {
			maxF1 = 0.0;
			for (Integer trueEdge : trueHyperedge) {
				F1 = computeF1(trueEdge, predictedEdge);
				if (F1 > maxF1) {
					maxF1 = F1;
				}
			}
			avgF1_1 += maxF1;
		}

		double avgF1_2 = 0.0;
		for (Integer trueEdge : trueHyperedgeSize) {
			maxF1 = 0.0;
			for (Integer predictedEdge : predictedHyperedge) {
				F1 = computeF1(trueEdge, predictedEdge);
				if (F1 > maxF1) {
					maxF1 = F1;
				}
			}
			avgF1_2 += maxF1;
		}

		return (avgF1_1 / (double) predictedHyperedge.size()+avgF1_2 / (double) trueHyperedge.size()) / 2.0;
	}

	public static List<List<Integer>> generateCombinations(List<Integer> array, int k) {
		List<List<Integer>> combinations = new ArrayList<>();
		generateCombinations(array, k, 0, new ArrayList<>(), combinations);
		return combinations;
	}

	private static void generateCombinations(List<Integer> array, int k, int index, List<Integer> current, List<List<Integer>> combinations) {
		if (current.size() == k) {
			combinations.add(new ArrayList<>(current));
			return;
		}

		if (index == array.size()) {
			return;
		}

		current.add(array.get(index));
		generateCombinations(array, k, index+1, current, combinations);

		current.remove(current.size()-1);
		generateCombinations(array, k, index+1, current, combinations);
	}

	public double boundFunction1(List<Integer> prefix, int prefixLength, Set<Integer> edgeSet, int support) {
		int epsilonP;
		if (faultV > prefixLength+1 && faultP > maxLenHyperedge) {
			epsilonP = (int) Math.floor(Math.min((double) (support*(prefixLength+1)) / (double) (faultV-prefixLength-1), (double) (support*maxLenHyperedge) / (double) (faultP-maxLenHyperedge)));
		} else if (faultV > prefixLength+1) {
			epsilonP = (int) Math.floor((double) (support*(prefixLength+1)) / (double) (faultV-prefixLength-1));
		} else if (faultP > maxLenHyperedge) {
			epsilonP = (int) Math.floor((double) (support*maxLenHyperedge) / (double) (faultP-maxLenHyperedge));
		} else {
			epsilonP = Integer.MAX_VALUE;
		}

		Set<Integer> edgeSetErrors = new HashSet<>();
		Set<Integer> edgeSetError1 = new HashSet<>();
		for (int iError = 0; iError < prefixLength+1; iError++) {
			for (int iPrefix = 0; iPrefix < prefixLength+1; iPrefix++) {
				if (iPrefix == iError) {
					continue;
				}
				if (iPrefix == 0 || (iError == 0 && iPrefix == 1)) {
					edgeSetError1 = new HashSet<>(nodeEdgeSetGround.get(prefix.get(iPrefix)));
					edgeSetError1.removeAll(edgeSet);
				} else {
					edgeSetError1.retainAll(nodeEdgeSetGround.get(prefix.get(iPrefix)));
				}
				if (edgeSetError1.isEmpty()) {
					break;
				}
			}
			edgeSetErrors.addAll(edgeSetError1);
		}
		double supportErrors = Math.min(edgeSetErrors.size(), epsilonP);

		double bound = 0.0;
		List<Double> boundList;
		if (supportErrors < epsilonP) {
			if (growth != 0.0) {
				for (Integer edgeNum : edgeSetErrors) {
					bound += (double) prefixLength / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
				}
			} else {
				for (Integer edgeNum : edgeSetErrors) {
					bound += (double) prefixLength / (double) groundLen.get(edgeNum);
				}
			}
		} else {
			boundList = new ArrayList<>();
			if (growth != 0.0) {
				for (Integer edgeNum : edgeSetErrors) {
					boundList.add((double) prefixLength / (double) groundLen.get(edgeNum) * decayTime[edgeNum]);
				}
			} else {
				for (Integer edgeNum : edgeSetErrors) {
					boundList.add((double) prefixLength / (double) groundLen.get(edgeNum));
				}
			}
			Collections.reverse(boundList);
			for (int i = 0; i < epsilonP; i++) {
				bound += boundList.get(i);
			}
		}

		double sumNumErrors;
		Set<Integer> edgeSetNumErrors;
		Set<Integer> edgeSetErrorsIdxs = new HashSet<>();
		boolean edgeSetErrorsIdxsNew = true;
		double supportNumErrors;
		if (epsilonE > 1 && supportErrors < epsilonP) {
			sumNumErrors = supportErrors;
			for (int numErrors = 2; numErrors < epsilonE+1; numErrors++) {
				if (numErrors*faultV > prefixLength+1 && numErrors*faultP > maxLenHyperedge) {
					epsilonP = (int) Math.floor(Math.min(((double) (prefixLength+1)*((double) support+supportErrors)-(double) faultV*sumNumErrors) / (double) (numErrors*faultV-prefixLength-1), ((double) maxLenHyperedge*((double) support+supportErrors)-sumNumErrors) / (double) (numErrors*faultP-maxLenHyperedge)));
				} else if (numErrors*faultV > prefixLength+1) {
					epsilonP = (int) Math.floor(((double) (prefixLength+1)*((double) support+supportErrors)-(double) faultV*sumNumErrors) / (double) (numErrors*faultV-prefixLength-1));
				} else if (numErrors*faultP > maxLenHyperedge) {
					epsilonP = (int) Math.floor(((double) maxLenHyperedge*((double) support+supportErrors)-sumNumErrors) / (double) (numErrors*faultP-maxLenHyperedge));
				} else {
					epsilonP = Integer.MAX_VALUE;
				}
				edgeSetNumErrors = new HashSet<>();
				for (List<Integer> numErrorsIdxs : generateCombinations(IntStream.range(0, prefixLength+1).boxed().collect(Collectors.toList()), numErrors)) {
					for (int iPrefix = 0; iPrefix < prefixLength+1; iPrefix++) {
						if (numErrorsIdxs.contains(iPrefix)) {
							continue;
						}
						if (edgeSetErrorsIdxsNew) {
							edgeSetErrorsIdxs = new HashSet<>(nodeEdgeSetGround.get(prefix.get(iPrefix)));
							edgeSetErrorsIdxs.removeAll(edgeSetErrors);
							edgeSetErrorsIdxs.removeAll(edgeSet);
							edgeSetErrorsIdxsNew = false;
						} else {
							edgeSetErrorsIdxs.retainAll(nodeEdgeSetGround.get(prefix.get(iPrefix)));
						}
						if (edgeSetErrorsIdxs.isEmpty()) {
							edgeSetErrorsIdxsNew = true;
							break;
						}
						edgeSetNumErrors.addAll(edgeSetErrorsIdxs);
					}
				}
				if (!edgeSetNumErrors.isEmpty()) {
					supportNumErrors = Math.min(edgeSetNumErrors.size(), epsilonP);
					supportErrors += supportNumErrors;
					if (supportNumErrors < epsilonP) {
						if (growth != 0.0) {
							for (Integer edgeNum : edgeSetNumErrors) {
								bound += (double) (prefixLength+1-numErrors) / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
							}
						} else {
							for (Integer edgeNum : edgeSetNumErrors) {
								bound += (double) (prefixLength+1-numErrors) / (double) groundLen.get(edgeNum);
							}
						}
					} else {
						boundList = new ArrayList<>();
						if (growth != 0.0) {
							for (Integer edgeNum : edgeSetNumErrors) {
								boundList.add((double) (prefixLength+1-numErrors) / (double) groundLen.get(edgeNum) * decayTime[edgeNum]);
							}
						} else {
							for (Integer edgeNum : edgeSetNumErrors) {
								boundList.add((double) (prefixLength+1-numErrors) / (double) groundLen.get(edgeNum));
							}
						}
						Collections.reverse(boundList);
						for (int i = 0; i < epsilonP; i++) {
							bound += boundList.get(i);
						}
					}
					if (supportNumErrors == epsilonP) {
						break;
					}
					sumNumErrors += (double) numErrors*supportNumErrors;
					edgeSetErrors.addAll(edgeSetNumErrors);
				}
			}
		}
		return bound;
	}

	public double boundFunction12(List<Integer> prefix, int prefixLength, int node1, Set<Integer> edgeSet1, int node2, Set<Integer> edgeSet2, Set<Integer> edgeSet12) {
		int epsilonP;
		int support12 = edgeSet12.size();
		if (faultV > prefixLength+2 && faultP > maxLenHyperedge) {
			epsilonP = (int) Math.floor(Math.min((double) (support12*(prefixLength+2)) / (double) (faultV-prefixLength-2), (double) (support12*maxLenHyperedge) / (double) (faultP-maxLenHyperedge)));
		} else if (faultV > prefixLength+2) {
			epsilonP = (int) Math.floor((double) (support12*(prefixLength+2)) / (double) (faultV-prefixLength-2));
		} else if (faultP > maxLenHyperedge) {
			epsilonP = (int) Math.floor((double) (support12*maxLenHyperedge) / (double) (faultP-maxLenHyperedge));
		} else {
			epsilonP = Integer.MAX_VALUE;
		}

		Set<Integer> edgeSetErrors12 = new HashSet<>(edgeSet1);
		edgeSetErrors12.addAll(edgeSet2);
		Set<Integer> edgeSetErrors = new HashSet<>(edgeSetErrors12);
		edgeSetErrors.removeAll(edgeSet12);
		Set<Integer> edgeSetNode12 = new HashSet<>(nodeEdgeSetGround.get(node1));
		edgeSetNode12.retainAll(nodeEdgeSetGround.get(node2));
		edgeSetNode12.removeAll(edgeSetErrors12);
		Set<Integer> edgeSetError1;

		if (!edgeSetNode12.isEmpty()) {
			for (int iError = 0; iError < prefixLength; iError++) {
				edgeSetError1 = new HashSet<>(edgeSetNode12);
				for (int iPrefix = 0; iPrefix < prefixLength; iPrefix++) {
					if (iPrefix == iError) {
						continue;
					}
					edgeSetError1.retainAll(nodeEdgeSetGround.get(prefix.get(iPrefix)));
					if (edgeSetError1.isEmpty()) {
						break;
					}
				}
				if (!edgeSetError1.isEmpty()) {
					edgeSetNode12.removeAll(edgeSetError1);
					edgeSetErrors.addAll(edgeSetError1);
					if (edgeSetNode12.isEmpty()) {
						break;
					}
				}
			}
		}
		double supportErrors = Math.min(edgeSetErrors.size(), epsilonP);

		double bound = 0.0;
		List<Double> boundList;
		if (supportErrors < epsilonP) {
			if (growth != 0.0) {
				for (Integer edgeNum : edgeSetErrors) {
					bound += (double) prefixLength / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
				}
			} else {
				for (Integer edgeNum : edgeSetErrors) {
					bound += (double) prefixLength / (double) groundLen.get(edgeNum);
				}
			}
		} else {
			boundList = new ArrayList<>();
			if (growth != 0.0) {
				for (Integer edgeNum : edgeSetErrors) {
					boundList.add((double) prefixLength / (double) groundLen.get(edgeNum) * decayTime[edgeNum]);
				}
			} else {
				for (Integer edgeNum : edgeSetErrors) {
					boundList.add((double) prefixLength / (double) groundLen.get(edgeNum));
				}
			}
			Collections.reverse(boundList);
			for (int i = 0; i < epsilonP; i++) {
				bound += boundList.get(i);
			}
		}

		double sumNumErrors;
		Set<Integer> edgeSetNumErrors;
		Set<Integer> edgeSetErrorsIdxs = new HashSet<>();
		boolean edgeSetErrorsIdxsNew = true;
		double supportNumErrors;
		List<Integer> prefixNode12;

		if (epsilonE > 1 && supportErrors < epsilonP) {
			sumNumErrors = supportErrors;
			prefixNode12 = new ArrayList<>(prefix);
			prefixNode12.add(node1);
			prefixNode12.add(node2);
			for (int numErrors = 2; numErrors < epsilonE+1; numErrors++) {
				if (numErrors*faultV > prefixLength+2 && numErrors*faultP > maxLenHyperedge) {
					epsilonP = (int) Math.floor(Math.min(((double) (prefixLength+2)*((double) support12+supportErrors)-(double) faultV*sumNumErrors) / (double) (numErrors*faultV-prefixLength-2), ((double) maxLenHyperedge*((double) support12+supportErrors)-sumNumErrors) / (double) (numErrors*faultP-maxLenHyperedge)));
				} else if (numErrors*faultV > prefixLength+2) {
					epsilonP = (int) Math.floor(((double) (prefixLength+2)*((double) support12+supportErrors)-(double) faultV*sumNumErrors) / (double) (numErrors*faultV-prefixLength-2));
				} else if (numErrors*faultP > maxLenHyperedge) {
					epsilonP = (int) Math.floor(((double) maxLenHyperedge*((double) support12+supportErrors)-sumNumErrors) / (double) (numErrors*faultP-maxLenHyperedge));
				} else {
					epsilonP = Integer.MAX_VALUE;
				}
				edgeSetNumErrors = new HashSet<>();
				for (List<Integer> numErrorsIdxs : generateCombinations(IntStream.range(0, prefixLength+2).boxed().collect(Collectors.toList()), numErrors)) {
					for (int iPrefix = 0; iPrefix < prefixLength+2; iPrefix++) {
						if (numErrorsIdxs.contains(iPrefix)) {
							continue;
						}
						if (edgeSetErrorsIdxsNew) {
							edgeSetErrorsIdxs = new HashSet<>(nodeEdgeSetGround.get(prefixNode12.get(iPrefix)));
							edgeSetErrorsIdxs.removeAll(edgeSetErrors);
							edgeSetErrorsIdxs.removeAll(edgeSet12);
							edgeSetErrorsIdxsNew = false;
						} else {
							edgeSetErrorsIdxs.retainAll(nodeEdgeSetGround.get(prefixNode12.get(iPrefix)));
						}
						if (edgeSetErrorsIdxs.isEmpty()) {
							edgeSetErrorsIdxsNew = true;
							break;
						}
						edgeSetNumErrors.addAll(edgeSetErrorsIdxs);
					}
				}
				if (!edgeSetNumErrors.isEmpty()) {
					supportNumErrors = Math.min(edgeSetNumErrors.size(), epsilonP);
					supportErrors += supportNumErrors;
					if (supportNumErrors < epsilonP) {
						if (growth != 0.0) {
							for (Integer edgeNum : edgeSetNumErrors) {
								bound += (double) (prefixLength+2-numErrors) / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
							}
						} else {
							for (Integer edgeNum : edgeSetNumErrors) {
								bound += (double) (prefixLength+2-numErrors) / (double) groundLen.get(edgeNum);
							}
						}
					} else {
						boundList = new ArrayList<>();
						if (growth != 0.0) {
							for (Integer edgeNum : edgeSetNumErrors) {
								boundList.add((double) (prefixLength+2-numErrors) / (double) groundLen.get(edgeNum) * decayTime[edgeNum]);
							}
						} else {
							for (Integer edgeNum : edgeSetNumErrors) {
								boundList.add((double) (prefixLength+2-numErrors) / (double) groundLen.get(edgeNum));
							}
						}
						Collections.reverse(boundList);
						for (int i = 0; i < epsilonP; i++) {
							bound += boundList.get(i);
						}
					}
					if (supportNumErrors == epsilonP) {
						break;
					}
					sumNumErrors += (double) numErrors*supportNumErrors;
					edgeSetErrors.addAll(edgeSetNumErrors);
				}
			}
		}
		return bound;
	}

	public int getSupport12(Set<Integer> edgeSet1, Set<Integer> edgeSet2) {
		Set<Integer> a;
		Set<Integer> b;
		if (edgeSet1.size() <= edgeSet2.size()) {
			a = edgeSet1;
			b = edgeSet2;
		} else {
			a = edgeSet2;
			b = edgeSet1;
		}
		int count = 0;
		for (Integer e : a) {
			if (b.contains(e)) {
				count++;
			}
		}
		return count;
	}

	public void processAlgorithm() {
		maxLenHyperedge = Collections.max(hyperedgeSizeCount.keySet());
		epsilonE = (int) Math.floor((double) maxLenHyperedge / (double) faultE);
		HOISupport.add(new HashMap<>());
		HOICount = new int[maxLenHyperedge-2];
		HOIMinsup = new double[maxLenHyperedge-2];
		for (int i = 0; i < maxLenHyperedge-2; i++) {
			HOISupport.add(new HashMap<>());
			HOICount[i] = 0;
			HOIMinsup[i] = Double.MIN_VALUE;
		}
		int hashedMapSize = hashedMap.size();
		if (hashedMapSize < maxLenHyperedge-2) {
			for (int i = hashedMapSize; i < maxLenHyperedge-2; i++) {
				hashedMap.add(new HashMap<>());
			}
		}

		Set<Integer> edgeSet1;
		Set<Integer> edgeSet2;
		List<Integer> equivalenceClass1Nodes;
		List<Set<Integer>> equivalenceClass1EdgeSets;
		List<Entry<Integer, Set<Integer>>> entryList = new ArrayList<>(nodeEdgeSetGround.entrySet());
		int support12;
		List<Integer> prefixNode1;

		int entryListSize = entryList.size();
		for (int i1 = 0; i1 < entryListSize; i1++) {
			edgeSet1 = entryList.get(i1).getValue();
			equivalenceClass1Nodes = new ArrayList<>();
			equivalenceClass1EdgeSets = new ArrayList<>();
			for (int i2 = i1+1; i2 < entryListSize; i2++) {
				edgeSet2 = entryList.get(i2).getValue();
				support12 = getSupport12(edgeSet1, edgeSet2);
				if (support12 > 0) {
					Set<Integer> edgeSet12 = new HashSet<>(edgeSet1);
					edgeSet12.retainAll(edgeSet2);
					equivalenceClass1Nodes.add(entryList.get(i2).getKey());
					equivalenceClass1EdgeSets.add(edgeSet12);
				}
			}
			if (!equivalenceClass1Nodes.isEmpty()) {
				prefixNode1 = new ArrayList<>();
				prefixNode1.add(entryList.get(i1).getKey());
				processEquivalenceClass(prefixNode1, 1, equivalenceClass1Nodes, equivalenceClass1EdgeSets);
			}
		}
	}

	public double calculateS(List<Integer> prefix, int prefixLength, Set<Integer> edgeSet, int support) {
		int epsilonP;
		if (faultV > prefixLength+1 && faultP > prefixLength+1) {
			epsilonP = (int) Math.floor(Math.min((double) (support*(prefixLength+1)) / (double) (faultV-prefixLength-1), (double) (support*(prefixLength+1)) / (double) (faultP-prefixLength-1)));
		} else if (faultV > prefixLength+1) {
			epsilonP = (int) Math.floor((double) (support*(prefixLength+1)) / (double) (faultV-prefixLength-1));
		} else if (faultP > prefixLength+1) {
			epsilonP = (int) Math.floor((double) (support*(prefixLength+1)) / (double) (faultP-prefixLength-1));
		} else {
			epsilonP = Integer.MAX_VALUE;
		}

		Set<Integer> edgeSetErrors = new HashSet<>();
		Set<Integer> edgeSetError1 = new HashSet<>();
		for (int iError = 0; iError < prefixLength+1; iError++) {
			for (int iPrefix = 0; iPrefix < prefixLength+1; iPrefix++) {
				if (iPrefix == iError) {
					continue;
				}
				if (iPrefix == 0 || (iError == 0 && iPrefix == 1)) {
					edgeSetError1 = new HashSet<>(nodeEdgeSetGround.get(prefix.get(iPrefix)));
					edgeSetError1.removeAll(edgeSet);
				} else {
					edgeSetError1.retainAll(nodeEdgeSetGround.get(prefix.get(iPrefix)));
				}
				if (edgeSetError1.isEmpty()) {
					break;
				}
			}
			edgeSetErrors.addAll(edgeSetError1);
		}
		double supportErrors = Math.min(edgeSetErrors.size(), epsilonP);
		int maxNumErrors = (int) Math.floor((double) (prefixLength+1) / (double) faultE);
		double sumNumErrors;
		Set<Integer> edgeSetNumErrors;
		Set<Integer> edgeSetErrorsIdxs = new HashSet<>();
		boolean edgeSetErrorsIdxsNew = true;
		double supportNumErrors;
		if (maxNumErrors > 1 && supportErrors < epsilonP) {
			sumNumErrors = supportErrors;
			for (int numErrors = 2; numErrors < maxNumErrors+1; numErrors++) {
				if (numErrors*faultV > prefixLength+1 && numErrors*faultP > prefixLength+1) {
					epsilonP = (int) Math.floor(Math.min(((double) (prefixLength+1)*((double) support+supportErrors)-(double) faultV*sumNumErrors) / (double) (numErrors*faultV-prefixLength-1), ((double) (prefixLength+1)*((double) support+supportErrors)-(double) faultP*sumNumErrors) / (double) (numErrors*faultP-prefixLength-1)));
				} else if (numErrors*faultV > prefixLength+1) {
					epsilonP = (int) Math.floor(((double) (prefixLength+1)*((double) support+supportErrors)-(double) faultV*sumNumErrors) / (double) (numErrors*faultV-prefixLength-1));
				} else if (numErrors*faultP > prefixLength+1) {
					epsilonP = (int) Math.floor(((double) (prefixLength+1)*((double) support+supportErrors)-(double) faultP*sumNumErrors) / (double) (numErrors*faultP-prefixLength-1));
				} else {
					epsilonP = Integer.MAX_VALUE;
				}
				edgeSetNumErrors = new HashSet<>();
				for (List<Integer> numErrorsIdxs : generateCombinations(IntStream.range(0, prefixLength+1).boxed().collect(Collectors.toList()), numErrors)) {
					for (int iPrefix = 0; iPrefix < prefixLength+1; iPrefix++) {
						if (numErrorsIdxs.contains(iPrefix)) {
							continue;
						}
						if (edgeSetErrorsIdxsNew) {
							edgeSetErrorsIdxs = new HashSet<>(nodeEdgeSetGround.get(prefix.get(iPrefix)));
							edgeSetErrorsIdxs.removeAll(edgeSetErrors);
							edgeSetErrorsIdxs.removeAll(edgeSet);
							edgeSetErrorsIdxsNew = false;
						} else {
							edgeSetErrorsIdxs.retainAll(nodeEdgeSetGround.get(prefix.get(iPrefix)));
						}
						if (edgeSetErrorsIdxs.isEmpty()) {
							edgeSetErrorsIdxsNew = true;
							break;
						}
						edgeSetNumErrors.addAll(edgeSetErrorsIdxs);
					}
				}
				if (!edgeSetNumErrors.isEmpty()) {
					supportNumErrors = Math.min(edgeSetNumErrors.size(), epsilonP);
					supportErrors += supportNumErrors;
					if (supportNumErrors == epsilonP) {
						break;
					}
					sumNumErrors += (double) numErrors*supportNumErrors;
					edgeSetErrors.addAll(edgeSetNumErrors);
				}
			}
		}
		return support+supportErrors;
	}

	public double[] calculateHOISup(List<Integer> prefix, int prefixLength, Set<Integer> edgeSet, double s) {
		int epsilonV = (int) Math.floor(s / (double) faultV);
		int epsilonP = (int) Math.floor(((double) (prefixLength+1)*s) / (double) faultP);
		Set<Integer> edgeSetErrors = new HashSet<>();
		Set<Integer> edgeSetError1 = new HashSet<>();
		double sumSupportErrors = 0.0;
		double supportError1;
		List<Integer> errorsIdxs = new ArrayList<>();
		List<Double> bU = new ArrayList<>();
		for (int iError = 0; iError < prefixLength+1; iError++) {
			for (int iPrefix = 0; iPrefix < prefixLength+1; iPrefix++) {
				if (iPrefix == iError) {
					continue;
				}
				if (iPrefix == 0 || (iError == 0 && iPrefix == 1)) {
					edgeSetError1 = new HashSet<>(nodeEdgeSetGround.get(prefix.get(iPrefix)));
					edgeSetError1.removeAll(edgeSet);
				} else {
					edgeSetError1.retainAll(nodeEdgeSetGround.get(prefix.get(iPrefix)));
				}
				if (edgeSetError1.isEmpty()) {
					break;
				}
			}
			supportError1 = Math.min(edgeSetError1.size(), epsilonV);
			if (!edgeSetError1.isEmpty()) {
				sumSupportErrors += supportError1;
				edgeSetErrors.addAll(edgeSetError1);
			}
			if (supportError1 < epsilonV) {
				bU.add((double) epsilonV-supportError1);
			} else {
				errorsIdxs.add(iError);
			}
		}
		sumSupportErrors = Math.min(sumSupportErrors, epsilonP);

		double sumSupport = 0.0;
		List<Double> sumSupportList;
		if (sumSupportErrors < epsilonP) {
			if (growth != 0.0) {
				for (Integer edgeNum : edgeSetErrors) {
					sumSupport += (double) prefixLength / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
				}
			} else {
				for (Integer edgeNum : edgeSetErrors) {
					sumSupport += (double) prefixLength / (double) groundLen.get(edgeNum);
				}
			}
		} else {
			sumSupportList = new ArrayList<>();
			if (growth != 0.0) {
				for (Integer edgeNum : edgeSetErrors) {
					sumSupportList.add((double) prefixLength / (double) groundLen.get(edgeNum) * decayTime[edgeNum]);
				}
			} else {
				for (Integer edgeNum : edgeSetErrors) {
					sumSupportList.add((double) prefixLength / (double) groundLen.get(edgeNum));
				}
			}
			Collections.reverse(sumSupportList);
			for (int i = 0; i < epsilonP; i++) {
				sumSupport += sumSupportList.get(i);
			}
		}

		int maxNumErrors = (int) Math.floor((double) (prefixLength+1) / (double) faultE);
		Set<Integer> edgeSetNumErrors;
		Set<Integer> edgeSetErrorsIdxs = new HashSet<>();
		boolean edgeSetErrorsIdxsNew = true;
		List<Set<Integer>> edgeSetErrorsList = new ArrayList<>();
		List<List<Integer>> A = new ArrayList<>();
		List<Integer> ANewRow;
		int ASize = 0;
		int colCount = 0;
		List<List<Integer>> ATransposed;
		List<Integer> AAny1;
		int AAny1Size = 0;
		List<List<Integer>> ATransposedAny1;
		List<Integer> ATransposedRow;

		MPSolver solver;
		MPVariable[] x;
		MPConstraint constraint;
		MPObjective objective;
		MPSolver.ResultStatus resultStatus;
		if (!bU.isEmpty() && maxNumErrors > 1 && sumSupportErrors < epsilonP) {
			for (int numErrors = 2; numErrors < maxNumErrors+1; numErrors++) {
				edgeSetNumErrors = new HashSet<>();
				for (List<Integer> numErrorsIdxs : generateCombinations(IntStream.range(0, prefixLength+1).boxed().collect(Collectors.toList()), numErrors)) {
					for (int iPrefix = 0; iPrefix < prefixLength+1; iPrefix++) {
						if (numErrorsIdxs.contains(iPrefix)) {
							continue;
						}
						if (edgeSetErrorsIdxsNew) {
							edgeSetErrorsIdxs = new HashSet<>(nodeEdgeSetGround.get(prefix.get(iPrefix)));
							edgeSetErrorsIdxs.removeAll(edgeSetErrors);
							edgeSetErrorsIdxs.removeAll(edgeSet);
							edgeSetErrorsIdxsNew = false;
						} else {
							edgeSetErrorsIdxs.retainAll(nodeEdgeSetGround.get(prefix.get(iPrefix)));
						}
						if (edgeSetErrorsIdxs.isEmpty()) {
							edgeSetErrorsIdxsNew = true;
							break;
						}
					}
					if (!edgeSetErrorsIdxs.isEmpty()) {
						edgeSetNumErrors.addAll(edgeSetErrorsIdxs);
						if (numErrorsIdxs.stream().noneMatch(errorsIdxs::contains)) {
							edgeSetErrorsList.add(edgeSetErrorsIdxs);
							ANewRow = new ArrayList<>();
							for (int idx = 0; idx < prefixLength+1; idx++) {
								if (errorsIdxs.contains(idx)) {
									continue;
								}
								if (!numErrorsIdxs.contains(idx)) {
									ANewRow.add(0);
								} else {
									ANewRow.add(1);
								}
							}
							ANewRow.add(numErrors);
							A.add(ANewRow);
							ASize++;
							if (ANewRow.size() > colCount) {
								colCount = ANewRow.size();
							}
						}
					}
				}
				if (!edgeSetNumErrors.isEmpty()) {
					edgeSetErrors.addAll(edgeSetNumErrors);
				}
			}
			if (ASize > 0) {
				ATransposed = new ArrayList<>();
				for (int i = 0; i < colCount; i++) {
					ATransposed.add(new ArrayList<>());
				}
				for (int i = 0; i < ASize; i++) {
					for (int j = 0; j < colCount; j++) {
						ATransposed.get(j).add(A.get(i).get(j));
					}
				}
				AAny1 = new ArrayList<>();
				ATransposedAny1 = new ArrayList<>();
				for (int i = 0; i < colCount; i++) {
					ATransposedRow = ATransposed.get(i);
					if (ATransposedRow.stream().anyMatch(a -> a > 0)) {
						AAny1.add(i);
						AAny1Size++;
						ATransposedAny1.add(ATransposedRow);
					}
				}

				solver = MPSolver.createSolver("SCIP");

				x = new MPVariable[ASize];
				for (int j = 0; j < ASize; ++j) {
					x[j] = solver.makeIntVar(0.0, Double.POSITIVE_INFINITY, "");
				}
				bU.add((double) epsilonP-sumSupportErrors);
				for (int i = 0; i < AAny1Size; ++i) {
					constraint = solver.makeConstraint(0, bU.get(AAny1.get(i)), "");
					for (int j = 0; j < ASize; ++j) {
						constraint.setCoefficient(x[j], ATransposedAny1.get(i).get(j));
					}
				}

				objective = solver.objective();
				for (int j = 0; j < ASize; ++j) {
					objective.setCoefficient(x[j], -1);
				}
				objective.setMaximization();
				resultStatus = solver.solve();

				sumSupportErrors -= objective.value();

				for (int idx = 0; idx < edgeSetErrorsList.size(); idx++) {
					sumSupportList = new ArrayList<>();
					if (growth != 0.0) {
						for (Integer edgeNum : edgeSetErrorsList.get(idx)) {
							sumSupportList.add((double) (prefixLength+1-ATransposedAny1.get(AAny1Size-1).get(idx)) / (double) groundLen.get(edgeNum) * decayTime[edgeNum]);
						}
					} else {
						for (Integer edgeNum : edgeSetErrorsList.get(idx)) {
							sumSupportList.add((double) (prefixLength+1-ATransposedAny1.get(AAny1Size-1).get(idx)) / (double) groundLen.get(edgeNum));
						}
					}
					Collections.reverse(sumSupportList);
					for (int i = 0; i < (int) x[idx].solutionValue(); i++) {
						sumSupport += sumSupportList.get(i);
					}
				}
			}
		}
		return new double[]{sumSupportErrors, sumSupport};
	}

	public double calculateSupport(List<Integer> prefix, int prefixLength, Set<Integer> edgeSet, int support, double HOISupInitial) {
		double s = calculateS(prefix, prefixLength, edgeSet, support);
		double[] sumSupport = calculateHOISup(prefix, prefixLength, edgeSet, s);
		double HOISup = (double) support + sumSupport[0];
		while (0.0 < HOISup && HOISup < s) {
			s = HOISup;
			sumSupport = calculateHOISup(prefix, prefixLength, edgeSet, s);
			HOISup = (double) support + sumSupport[0];
		}
		HOISup = HOISupInitial + sumSupport[1];
		return HOISup;
	}

	public void processHOI(List<Integer> HOI, double HOISup, int prefixLength) {
		Integer HOINumber = hyperedgeToHashed(HOI, prefixLength-1);
		if (!HOISupportNoSize.containsKey(HOISup)) {
			HOISupportNoSize.put(HOISup, new HashSet<>());
		}
		HOISupportNoSize.get(HOISup).add(HOINumber);

		Map<Double, Set<Integer>> HOISupport_1 = HOISupport.get(prefixLength-1);
		if (!HOISupport_1.containsKey(HOISup)) {
			HOISupport_1.put(HOISup, new HashSet<>());
		}
		HOISupport_1.get(HOISup).add(HOINumber);

		HOICountNoSize++;
		HOICount[prefixLength-2]++;

		int HOISupportMinsupNoSize;
		Set<Integer> HOISupportGetMinsupNoSize = HOISupportNoSize.get(HOIMinsupNoSize);
		if (HOISupportGetMinsupNoSize == null) {
			HOISupportMinsupNoSize = 0;
		} else {
			HOISupportMinsupNoSize = HOISupportGetMinsupNoSize.size();
		}
		int HOISupportMinsup;
		Set<Integer> HOISupportGetMinsup = HOISupport.get(prefixLength-1).get(HOIMinsup[prefixLength-2]);
		if (HOISupportGetMinsup == null) {
			HOISupportMinsup = 0;
		} else {
			HOISupportMinsup = HOISupportGetMinsup.size();
		}

		if ((HOICountNoSize-HOISupportMinsupNoSize) >= lenTestData) {
			HOISupportNoSize.remove(HOIMinsupNoSize);
			HOICountNoSize -= HOISupportMinsupNoSize;
		}

		if ((HOICount[prefixLength-2] > HOISupportMinsup) && (HOICount[prefixLength-2]-HOISupportMinsup >= hyperedgeSizeCount.get((short) (prefixLength+1))) && (prefixLength+1 == maxLenHyperedge || IntStream.range(prefixLength+1, maxLenHyperedge).allMatch(idx -> HOICount[idx-2] >= hyperedgeSizeCount.get((short) (idx+1))))) {
			HOISupport.get(prefixLength-1).remove(HOIMinsup[prefixLength-2]);
			HOICount[prefixLength-2] -= HOISupportMinsup;
			HOIMinsup[prefixLength-2] = Collections.min(HOISupport.get(prefixLength-1).keySet());
		}
	}

	public double calculateSize2HOISup(Set<Integer> edgeSet) {
		double HOISup = 0.0;
		if (growth != 0.0) {
			for (Integer edgeNum : edgeSet) {
				HOISup += 2.0 / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
			}
		} else {
			for (Integer edgeNum : edgeSet) {
				HOISup += 2.0 / (double) groundLen.get(edgeNum);
			}
		}
		return HOISup;
	}

	public double[] calculateHOISupBound(List<Integer> HOI, int prefixLength, Set<Integer> edgeSet1) {
		double HOISup = 0.0;
		if (growth != 0.0) {
			for (Integer edgeNum : edgeSet1) {
				HOISup += ((double) prefixLength+1.0) / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
			}
		} else {
			for (Integer edgeNum : edgeSet1) {
				HOISup += ((double) prefixLength+1.0) / (double) groundLen.get(edgeNum);
			}
		}
		double bound = HOISup;
		if (prefixLength+1 >= faultE) {
			int support1 = edgeSet1.size();
			bound += boundFunction1(HOI, prefixLength, edgeSet1, support1);
			HOISup = calculateSupport(HOI, prefixLength, edgeSet1, support1, HOISup);
		}
		return new double[]{HOISup, bound};
	}

	public double[] calculateHOI12SupBound(List<Integer> HOI, List<Integer> prefix, int prefixLength, int node1, Set<Integer> edgeSet1, int node2, Set<Integer> edgeSet2, Set<Integer> edgeSet12, double bound1, double bound2) {
		double HOISup = 0.0;
		if (growth != 0.0) {
			for (Integer edgeNum : edgeSet12) {
				HOISup += ((double) prefixLength+2.0) / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
			}
		} else {
			for (Integer edgeNum : edgeSet12) {
				HOISup += ((double) prefixLength+2.0) / (double) groundLen.get(edgeNum);
			}
		}
		double bound = HOISup;
		if (prefixLength+2 >= faultE) {
			int support12 = edgeSet12.size();
			bound += boundFunction12(prefix, prefixLength, node1, edgeSet1, node2, edgeSet2, edgeSet12);
			if ((prefixLength == 1 || (bound1 >= HOIMinsup[prefixLength-2] && bound2 >= HOIMinsup[prefixLength-2])) && bound >= HOIMinsup[prefixLength-1]) {
				HOISup = calculateSupport(HOI, prefixLength+1, edgeSet12, support12, HOISup);
			}
		}
		return new double[]{HOISup, bound};
	}

	public Integer hyperedgeToHashed(List<Integer> hyperedge, int k) {
		Map<List<Integer>, Integer> hashedMapGet = hashedMap.get(k-1);
		if (!hashedMapGet.containsKey(hyperedge)) {
			hashedList.add(hyperedge);
			hashedMapGet.put(hyperedge, hashedNumber);
			hashedNumber++;
			return hashedNumber-1;
		}
		return hashedMapGet.get(hyperedge);
	}

	public double calculateHOISoftSup(int prefixLength, Set<Integer> edgeSet12) {
		double HOISup = 0.0;
		if (growth != 0.0) {
			for (Integer edgeNum : edgeSet12) {
				HOISup += ((double) prefixLength+2.0) / (double) groundLen.get(edgeNum) * decayTime[edgeNum];
			}
		} else {
			for (Integer edgeNum : edgeSet12) {
				HOISup += ((double) prefixLength+2.0) / (double) groundLen.get(edgeNum);
			}
		}
		return HOISup;
	}

	public void processEquivalenceClass(List<Integer> prefix, int prefixLength, List<Integer> equivalenceClassNodes, List<Set<Integer>> equivalenceClassEdgeSets) {
		int node1;
		int nodePrefix;
		List<Integer> HOI;
		Integer HOINumber;
		double HOISup;
		Map<Double, Set<Integer>> HOISupport0;

		int equivalenceClassNodesSize = equivalenceClassNodes.size();
		if (equivalenceClassNodesSize == 1) {
			node1 = equivalenceClassNodes.get(0);
			if (prefixLength == 1) {
				nodePrefix = prefix.get(0);
				if (nodePrefix < node1) {
					HOINumber = nodePrefix * nv + node1;
				} else {
					HOINumber = node1 * nv + nodePrefix;
				}
				if (!ground2.contains(HOINumber)) {
					HOISup = calculateSize2HOISup(equivalenceClassEdgeSets.get(0));
					HOISupport0 = HOISupport.get(0);
					if (!HOISupport0.containsKey(HOISup)) {
						HOISupport0.put(HOISup, new HashSet<>());
					}
					HOISupport0.get(HOISup).add(HOINumber);
				}
			} else {
				HOI = new ArrayList<>(prefix);
				HOI.add(node1);
				Collections.sort(HOI);
				HOI = Collections.unmodifiableList(HOI);
				if (!groundSize.get(prefixLength-2).contains(HOI)) {
					HOISup = calculateHOISupBound(HOI, prefixLength, equivalenceClassEdgeSets.get(0))[0];
					if (HOISup >= HOIMinsup[prefixLength-2]) {
						processHOI(HOI, HOISup, prefixLength);
					}
				}
			}
			return;
		}

		int node2;
		Set<Integer> edgeSet1;
		Set<Integer> edgeSet2;
		Set<Integer> edgeSet12;
		double bound1;
		double bound2;
		double[] HOISupBound;

		if (equivalenceClassNodesSize == 2) {
			node1 = equivalenceClassNodes.get(0);
			edgeSet1 = equivalenceClassEdgeSets.get(0);
			bound1 = Double.MAX_VALUE;
			node2 = equivalenceClassNodes.get(1);
			edgeSet2 = equivalenceClassEdgeSets.get(1);
			bound2 = Double.MAX_VALUE;
			if (prefixLength == 1) {
				nodePrefix = prefix.get(0);
				if (nodePrefix < node1) {
					HOINumber = nodePrefix * nv + node1;
				} else {
					HOINumber = node1 * nv + nodePrefix;
				}
				if (!ground2.contains(HOINumber)) {
					HOISup = calculateSize2HOISup(edgeSet1);
					HOISupport0 = HOISupport.get(0);
					if (!HOISupport0.containsKey(HOISup)) {
						HOISupport0.put(HOISup, new HashSet<>());
					}
					HOISupport0.get(HOISup).add(HOINumber);
				}
				if (nodePrefix < node2) {
					HOINumber = nodePrefix * nv + node2;
				} else {
					HOINumber = node2 * nv + nodePrefix;
				}
				if (!ground2.contains(HOINumber)) {
					HOISup = calculateSize2HOISup(edgeSet2);
					HOISupport0 = HOISupport.get(0);
					if (!HOISupport0.containsKey(HOISup)) {
						HOISupport0.put(HOISup, new HashSet<>());
					}
					HOISupport0.get(HOISup).add(HOINumber);
				}
			} else {
				HOI = new ArrayList<>(prefix);
				HOI.add(node1);
				Collections.sort(HOI);
				HOI = Collections.unmodifiableList(HOI);
				if (!groundSize.get(prefixLength-2).contains(HOI)) {
					HOISupBound = calculateHOISupBound(HOI, prefixLength, edgeSet1);
					HOISup = HOISupBound[0];
					bound1 = HOISupBound[1];
					if (HOISup >= HOIMinsup[prefixLength-2]) {
						processHOI(HOI, HOISup, prefixLength);
					}
				}
				HOI = new ArrayList<>(prefix);
				HOI.add(node2);
				Collections.sort(HOI);
				HOI = Collections.unmodifiableList(HOI);
				if (!groundSize.get(prefixLength-2).contains(HOI)) {
					HOISupBound = calculateHOISupBound(HOI, prefixLength, edgeSet2);
					HOISup = HOISupBound[0];
					bound2 = HOISupBound[1];
					if (HOISup >= HOIMinsup[prefixLength - 2]) {
						processHOI(HOI, HOISup, prefixLength);
					}
				}
			}

			if (prefixLength+2 <= maxLenHyperedge) {
				HOI = new ArrayList<>(prefix);
				HOI.add(node1);
				HOI.add(node2);
				Collections.sort(HOI);
				HOI = Collections.unmodifiableList(HOI);
				if (!groundSize.get(prefixLength-1).contains(HOI)) {
					edgeSet12 = new HashSet<>(edgeSet1);
					edgeSet12.retainAll(edgeSet2);
					HOISup = calculateHOI12SupBound(HOI, prefix, prefixLength, node1, edgeSet1, node2, edgeSet2, edgeSet12, bound1, bound2)[0];
					if (HOISup >= HOIMinsup[prefixLength-1]) {
						processHOI(HOI, HOISup, prefixLength+1);
					}
				}
			}
			return;
		}

		double HOIMinsupBound;
		double HOIMinsupBound12;
		List<Integer> equivalenceClassISuffixNodes;
		List<Set<Integer>> equivalence1EdgeSets;
		List<Integer> prefixNode1;

		for (int i1 = 0; i1 < equivalenceClassNodesSize; i1++) {
			node1 = equivalenceClassNodes.get(i1);
			edgeSet1 = equivalenceClassEdgeSets.get(i1);
			bound1 = Double.MAX_VALUE;
			if (prefixLength == 1) {
				nodePrefix = prefix.get(0);
				if (nodePrefix < node1) {
					HOINumber = nodePrefix * nv + node1;
				} else {
					HOINumber = node1 * nv + nodePrefix;
				}
				if (!ground2.contains(HOINumber)) {
					HOISup = calculateSize2HOISup(edgeSet1);
					HOISupport0 = HOISupport.get(0);
					if (!HOISupport0.containsKey(HOISup)) {
						HOISupport0.put(HOISup, new HashSet<>());
					}
					HOISupport0.get(HOISup).add(HOINumber);
				}
			} else {
				HOI = new ArrayList<>(prefix);
				HOI.add(node1);
				Collections.sort(HOI);
				HOI = Collections.unmodifiableList(HOI);
				if (!groundSize.get(prefixLength-2).contains(HOI)) {
					HOISupBound = calculateHOISupBound(HOI, prefixLength, edgeSet1);
					HOISup = HOISupBound[0];
					bound1 = HOISupBound[1];
					if (HOISup >= HOIMinsup[prefixLength - 2]) {
						processHOI(HOI, HOISup, prefixLength);
					}
				}
			}

			if (prefixLength+2 <= maxLenHyperedge) {
				HOIMinsupBound = Double.MAX_VALUE;
				for (int i=Math.max(prefixLength-2, 0); i<maxLenHyperedge-2; i++) {
					if (HOIMinsup[i] < HOIMinsupBound) {
						HOIMinsupBound = HOIMinsup[i];
					}
				}
				if (bound1 >= HOIMinsupBound) {
					equivalenceClassISuffixNodes = new ArrayList<>();
					equivalence1EdgeSets = new ArrayList<>();
					for (int i2 = i1+1; i2 < equivalenceClassNodesSize; i2++) {
						node2 = equivalenceClassNodes.get(i2);
						edgeSet2 = equivalenceClassEdgeSets.get(i2);
						edgeSet12 = new HashSet<>(edgeSet1);
						edgeSet12.retainAll(edgeSet2);
						HOISup = calculateHOISoftSup(prefixLength, edgeSet12);
						if (HOISup >= HOIMinsup[prefixLength-1]) {
							equivalenceClassISuffixNodes.add(node2);
							equivalence1EdgeSets.add(edgeSet12);
						} else if (prefixLength+2 >= faultE){
							HOIMinsupBound12 = Double.MAX_VALUE;
							for (int i=prefixLength-1; i<maxLenHyperedge-2; i++) {
								if (HOIMinsup[i] < HOIMinsupBound12) {
									HOIMinsupBound12 = HOIMinsup[i];
								}
							}
							if (HOISup + boundFunction12(prefix, prefixLength, node1, edgeSet1, node2, edgeSet2, edgeSet12) >= HOIMinsupBound12) {
								equivalenceClassISuffixNodes.add(node2);
								equivalence1EdgeSets.add(edgeSet12);
							}
						}
					}
					if (!equivalenceClassISuffixNodes.isEmpty()) {
						prefixNode1 = new ArrayList<>(prefix);
						prefixNode1.add(node1);
						processEquivalenceClass(prefixNode1, prefixLength+1, equivalenceClassISuffixNodes, equivalence1EdgeSets);
					}
				}
			}
		}
	}
}
