/*
 * Copyright (c) 2014 Red Hat, Inc. and/or its affiliates.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Cheng Fang - Initial API and implementation
 */

package org.jberet.support.io;

/**
 * A bean type represents record in HallOfFame.txt, data from http://www.baseball-databank.org/
 */
public final class HallOfFame {
    private String hofID;
    private int yearID;
    private String votedBy;
    private int ballots;
    private String needed;
    private double votes;
    private boolean inducted;
    private String category;

    public String getHofID() {
        return hofID;
    }

    public void setHofID(final String hofID) {
        this.hofID = hofID;
    }

    public int getYearID() {
        return yearID;
    }

    public void setYearID(final int yearID) {
        this.yearID = yearID;
    }

    public String getVotedBy() {
        return votedBy;
    }

    public void setVotedBy(final String votedBy) {
        this.votedBy = votedBy;
    }

    public int getBallots() {
        return ballots;
    }

    public void setBallots(final int ballots) {
        this.ballots = ballots;
    }

    public String getNeeded() {
        return needed;
    }

    public void setNeeded(final String needed) {
        this.needed = needed;
    }

    public double getVotes() {
        return votes;
    }

    public void setVotes(final double votes) {
        this.votes = votes;
    }

    public boolean isInducted() {
        return inducted;
    }

    public void setInducted(final boolean inducted) {
        this.inducted = inducted;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HallOfFame{");
        sb.append("hofID='").append(hofID).append('\'');
        sb.append(", yearID=").append(yearID);
        sb.append(", votedBy='").append(votedBy).append('\'');
        sb.append(", ballots=").append(ballots);
        sb.append(", needed='").append(needed).append('\'');
        sb.append(", votes=").append(votes);
        sb.append(", inducted=").append(inducted);
        sb.append(", category='").append(category).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
