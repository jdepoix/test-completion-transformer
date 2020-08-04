package org.jdepoix.testrelationfinder.relation;

class RankingResult<T> implements Comparable<RankingResult<T>> {
    private final T entity;
    private final double score;

    public RankingResult(T entity, double score) {
        this.entity = entity;
        this.score = score;
    }

    public double getScore() {
        return score;
    }

    public T getEntity() {
        return entity;
    }

    @Override
    public int compareTo(RankingResult<T> o) {
        return Double.compare(this.getScore(), o.getScore());
    }

    @Override
    public String toString() {
        return "RankingResult{" +
            "score=" + score +
            ", entity=" + entity +
            '}';
    }
}
