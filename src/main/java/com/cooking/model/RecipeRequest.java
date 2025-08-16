package com.cooking.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class RecipeRequest implements Serializable {
    private final String query;
    private final String dietaryPreference;
    private final boolean needSubstitutions;

    @JsonCreator
    public RecipeRequest(@JsonProperty("query") String query,
                         @JsonProperty("dietaryPreference") String dietaryPreference,
                         @JsonProperty("needSubstitutions") boolean needSubstitutions) {
        this.query = query;
        this.dietaryPreference = dietaryPreference;
        this.needSubstitutions = needSubstitutions;
    }

    // Getters
    public String getQuery() {
        return query;
    }

    public String getDietaryPreference() {
        return dietaryPreference;
    }

    public boolean isNeedSubstitutions() {
        return needSubstitutions;
    }

    @Override
    public String toString() {
        return "RecipeRequest{" +
                "query='" + query + '\'' +
                ", dietaryPreference='" + dietaryPreference + '\'' +
                ", needSubstitutions=" + needSubstitutions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RecipeRequest that = (RecipeRequest) o;

        if (needSubstitutions != that.needSubstitutions) return false;
        if (!query.equals(that.query)) return false;
        return dietaryPreference != null ? dietaryPreference.equals(that.dietaryPreference) : that.dietaryPreference == null;
    }

    @Override
    public int hashCode() {
        int result = query.hashCode();
        result = 31 * result + (dietaryPreference != null ? dietaryPreference.hashCode() : 0);
        result = 31 * result + (needSubstitutions ? 1 : 0);
        return result;
    }
}