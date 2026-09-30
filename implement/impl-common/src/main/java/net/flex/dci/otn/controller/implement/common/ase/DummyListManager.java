package net.flex.dci.otn.controller.implement.common.ase;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DummyListManager {
    private final long NORMAL_DUMMY_WIDTH = 100000;
    private final long MIN_DUMMY_WIDTH = 50000;
    private final long ALIGN_STEP = 6250;

    private final List<FrequencyRange> supportedBands;
    private List<FrequencyRange> dummyOchList;
    private List<FrequencyRange> businessOchList;

    public DummyListManager(List<FrequencyRange> supportedBands, List<FrequencyRange> dummyOchs, List<FrequencyRange> businessOchList) {
        this.supportedBands = supportedBands;
        this.dummyOchList = dummyOchs;
        this.businessOchList = businessOchList;

        dummyOchList.sort(Comparator.comparingLong(FrequencyRange::getLower));

        log.debug("Init dummy och ({}): {}", dummyOchs.size(), dummyOchs);
        log.debug("Init business och ({}): {}", businessOchList.size(), businessOchList);
    }

    public Result addBusinessOch(FrequencyRange businessOch) {
        log.info("Adding business OCH: {}", businessOch);

        dummyOchList.sort(Comparator.comparingLong(FrequencyRange::getLower));
        log.debug("init dummy och ({}): {}", dummyOchList.size(), dummyOchList);
        List<FrequencyRange> initDummyOchList = new ArrayList<>(dummyOchList);

        List<FrequencyRange> removed = dummyOchList.stream()
                .filter(d -> d.overlaps(businessOch))
                .collect(Collectors.toList());
        log.debug("Removing overlapping dummy och: {}", removed);

        dummyOchList.removeAll(removed);
        businessOchList.add(businessOch);
        List<FrequencyRange> wholeOchList = new ArrayList<>(businessOchList);
        wholeOchList.addAll(dummyOchList);

        wholeOchList.sort(Comparator.comparingLong(FrequencyRange::getLower));
        List<FrequencyRange> added = new ArrayList<>();
        List<FrequencyRange> gaps = FrequencyRange.subtract(supportedBands, wholeOchList);
        for (FrequencyRange gap : gaps) {   //C+L两个波段 单独处理， 否则C+L中间的空虚也会被dummy 填充
            added.addAll(createDummyOch(gap));
        }
        log.debug("Added dummy och (raw): {}", added);

        // ------- 4. 过滤掉完全覆盖 current(dummyOchList) 的 dummy -------
        List<FrequencyRange> currentDummy = new ArrayList<>(dummyOchList);
        List<FrequencyRange> filteredAdded = filterFullyOverlapped(added, currentDummy);
        List<FrequencyRange> unchanged = filteredAdded.stream()
                .filter(removed::contains)
                .collect(Collectors.toList());

        removed.removeAll(unchanged);
        filteredAdded.removeAll(unchanged);
        log.debug("Unchanged dummy och: {}", unchanged);
        log.debug("Added dummy och (filtered): {}", filteredAdded);

        dummyOchList.addAll(filteredAdded);
        dummyOchList.sort(Comparator.comparingLong(FrequencyRange::getLower));
        log.debug("Updated dummy och ({}): {}", dummyOchList.size(), dummyOchList);

        return new Result(removed, filteredAdded);  //两个都是频率范围
    }

    private List<FrequencyRange> filterFullyOverlapped(List<FrequencyRange> added, List<FrequencyRange> currentDummy) {
        return added.stream()
                .filter(a -> currentDummy.stream()
                        .noneMatch(d -> isFullyOverlap(a, d)))
                .collect(Collectors.toList());
    }

    private boolean isFullyOverlap(FrequencyRange a, FrequencyRange b) {
        return a.getLower() >= b.getLower() && a.getUpper() <= b.getUpper();
    }

    //这个时候businessOch已经被删除了
    public Result removeBusinessOch(FrequencyRange businessOch) {
        log.info("Removing business OCH: {}", businessOch);

        // Remove the business OCH from the businessOchList
        businessOchList.removeIf(och -> och.lower == businessOch.lower && och.upper == businessOch.upper);

        List<FrequencyRange> wholeOchList = new ArrayList<>(businessOchList);
        wholeOchList.addAll(dummyOchList);
        wholeOchList.add(businessOch);

        wholeOchList = wholeOchList.stream().distinct()
                .sorted(Comparator.comparingLong(FrequencyRange::getLower)).collect(Collectors.toList());

        log.debug("before remove, the whole och ({}): {}", wholeOchList.size(), wholeOchList);

        //获取要删除的这个businessOch，在整个波普中的位置
        ListIterator<FrequencyRange> it = wholeOchList.listIterator();
        int removedIndex = -1;
        while (it.hasNext()) {
            int currentIndex = it.nextIndex();
            FrequencyRange x = it.next();

            if (x.getLower() == businessOch.getLower()
                    && x.getUpper() == businessOch.getUpper()) {
                removedIndex = currentIndex;
                break;
            }
        }

        long gapStart = businessOch.lower;
        long gapEnd = businessOch.upper;

        List<FrequencyRange> removed = new ArrayList<>();

        if (removedIndex >= 0) {
            gapStart = expandGapLeft(wholeOchList, removedIndex - 1, gapStart, removed);
            gapEnd = expandGapRight(wholeOchList, removedIndex + 1, gapEnd, removed);
        }

        // =========================
        // 重新生成 dummy
        // =========================
        FrequencyRange expandedGap = new FrequencyRange(gapStart, gapEnd);
        log.debug("Rebuild Gap: {}", expandedGap);

        List<FrequencyRange> validGaps = supportedBands.stream()
                .map(band -> intersect(band, expandedGap))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<FrequencyRange> added = new ArrayList<>();
        for (FrequencyRange gap : validGaps) {
            added.addAll(createDummyOch(gap));
        }

        log.debug("Added dummy och (raw): {}", added);

        List<FrequencyRange> overlappedDummy = dummyOchList.stream()
                .filter(d -> d.overlaps(expandedGap))
                .collect(Collectors.toList());
        addAllDistinct(removed, overlappedDummy);
        dummyOchList.removeAll(removed);

        // ------- 4. 过滤掉完全覆盖 current(dummyOchList) 的 dummy -------
        List<FrequencyRange> currentDummy = new ArrayList<>(dummyOchList);
        List<FrequencyRange> filteredAdded = filterFullyOverlapped(added, currentDummy);
        List<FrequencyRange> unchanged = filteredAdded.stream()
                .filter(removed::contains)
                .collect(Collectors.toList());

        removed.removeAll(unchanged);
        filteredAdded.removeAll(unchanged);
        log.debug("Unchanged dummy och: {}", unchanged);
        log.debug("Added dummy och (filtered): {}", filteredAdded);

        dummyOchList.addAll(filteredAdded);
        dummyOchList.sort(Comparator.comparingLong(FrequencyRange::getLower));

        log.debug("Removed dummy och: {}", removed);
        log.debug("Added dummy och: {}", filteredAdded);
        log.debug("Updated dummy och ({}): {}", dummyOchList.size(), dummyOchList);

        return new Result(removed, filteredAdded);
    }

    private long expandGapLeft(List<FrequencyRange> wholeOchList, int index, long gapStart, List<FrequencyRange> removed) {
        for (int i = index; i >= 0; i--) {
            FrequencyRange leftOch = wholeOchList.get(i);
            long gapWidth = gapStart - leftOch.upper;
            if (gapWidth > 0) {
                gapStart = leftOch.upper;
            }

            if (isDummy(leftOch) && width(leftOch) == MIN_DUMMY_WIDTH) {
                gapStart = Math.min(gapStart, leftOch.lower);
                addDistinct(removed, leftOch);
            } else {
                break;
            }
        }
        return gapStart;
    }

    private long expandGapRight(List<FrequencyRange> wholeOchList, int index, long gapEnd, List<FrequencyRange> removed) {
        for (int i = index; i < wholeOchList.size(); i++) {
            FrequencyRange rightOch = wholeOchList.get(i);
            long gapWidth = rightOch.lower - gapEnd;
            if (gapWidth > 0) {
                gapEnd = rightOch.lower;
            }

            if (isDummy(rightOch) && width(rightOch) == MIN_DUMMY_WIDTH) {
                gapEnd = Math.max(gapEnd, rightOch.upper);
                addDistinct(removed, rightOch);
            } else {
                break;
            }
        }
        return gapEnd;
    }

    private void addAllDistinct(List<FrequencyRange> target, List<FrequencyRange> source) {
        for (FrequencyRange range : source) {
            addDistinct(target, range);
        }
    }

    private void addDistinct(List<FrequencyRange> target, FrequencyRange range) {
        if (!target.contains(range)) {
            target.add(range);
        }
    }

    private FrequencyRange intersect(FrequencyRange a, FrequencyRange b) {
        long lower = Math.max(a.lower, b.lower);
        long upper = Math.min(a.upper, b.upper);
        return (lower < upper) ? new FrequencyRange(lower, upper) : null;
    }

    // 判断是不是 dummy
    private boolean isDummy(FrequencyRange och) {
        if (och == null) return false;
        return dummyOchList.stream()
                .anyMatch(b -> b.lower == och.lower && b.upper == och.upper);
    }

    // 计算波段宽度
    private long width(FrequencyRange fr) {
        return fr.upper - fr.lower;
    }

    //检查空洞是否可以创建100GHz, 尽量多的找出来，然后再看50GHz
    private List<FrequencyRange> createDummyOch(FrequencyRange range) {
        List<FrequencyRange> result = new ArrayList<>();
        long current = alignToGrid(range.lower);

        while (current + NORMAL_DUMMY_WIDTH <= range.upper) {
            result.add(new FrequencyRange(current, current + NORMAL_DUMMY_WIDTH));
            current += NORMAL_DUMMY_WIDTH;
        }

//        if (range.upper - current >= MIN_DUMMY_WIDTH) {
//            result.add(new FrequencyRange(current, current + MIN_DUMMY_WIDTH));
//        }
        // 上面的判断还是会有超范围的出现。 确保不会超出 range.upper
        long dummyEnd = current + MIN_DUMMY_WIDTH;
        if (dummyEnd <= range.upper) {
            result.add(new FrequencyRange(current, dummyEnd));
        }


        return result;
    }

    private long alignToGrid(long freq) {
        return freq - (freq % ALIGN_STEP); // Align to 6.25GHz grid (rounded to 6GHz here)
    }

    // Inner helper classes
    @Data
    public static class FrequencyRange {
        public final long lower;
        public final long upper;

        public FrequencyRange(long lower, long upper) {
            this.lower = lower;
            this.upper = upper;
        }

        public boolean overlaps(FrequencyRange other) {
            return this.lower < other.upper && other.lower < this.upper;
        }

        public static List<FrequencyRange> subtract(List<FrequencyRange> bands, List<FrequencyRange> ochs) {
            List<FrequencyRange> result = new ArrayList<>();
            for (FrequencyRange band : bands) {
                List<FrequencyRange> fragments = new ArrayList<>();
                fragments.add(band);
                for (FrequencyRange och : ochs) {
                    fragments = subtractOneOch(fragments, och);
                }
                result.addAll(fragments);
            }
            return result;
        }

        //在波段范围内，逐一减去已经占用的频段(och）， result 就是频谱空洞
        private static List<FrequencyRange> subtractOneOch(List<FrequencyRange> inputs, FrequencyRange och) {
            List<FrequencyRange> result = new ArrayList<>();
            for (FrequencyRange range : inputs) {
                if (!range.overlaps(new FrequencyRange(och.lower, och.upper))) {
                    result.add(range);
                } else {
                    if (range.lower < och.lower) {
                        result.add(new FrequencyRange(range.lower, och.lower));
                    }
                    if (och.upper < range.upper) {
                        result.add(new FrequencyRange(och.upper, range.upper));
                    }
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return "[" + lower + "," + upper + "]";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FrequencyRange)) return false;
            FrequencyRange och = (FrequencyRange) o;
            return lower == och.lower && upper == och.upper;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lower, upper);
        }
    }

    @Data
    public static class Result {
        public final List<FrequencyRange> removed;
        public final List<FrequencyRange> added;

        public Result(List<FrequencyRange> removed, List<FrequencyRange> added) {
            this.removed = removed;
            this.added = added;
        }

        @Override
        public String toString() {
            return "Removed: " + removed + ", Added: " + added;
        }
    }
}
