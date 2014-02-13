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

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
        "id",
        "name",
        "full_name",
        "owner",
        "private",
        "html_url",
        "description",
        "fork",
        "url",
        "forks_url",
        "keys_url",
        "collaborators_url",
        "teams_url",
        "hooks_url",
        "issue_events_url",
        "events_url",
        "assignees_url",
        "branches_url",
        "tags_url",
        "blobs_url",
        "git_tags_url",
        "git_refs_url",
        "trees_url",
        "statuses_url",
        "languages_url",
        "stargazers_url",
        "contributors_url",
        "subscribers_url",
        "subscription_url",
        "commits_url",
        "git_commits_url",
        "comments_url",
        "issue_comment_url",
        "contents_url",
        "compare_url",
        "merges_url",
        "archive_url",
        "downloads_url",
        "issues_url",
        "pulls_url",
        "milestones_url",
        "notifications_url",
        "labels_url",
        "releases_url",
        "created_at",
        "updated_at",
        "pushed_at",
        "git_url",
        "ssh_url",
        "clone_url",
        "svn_url",
        "homepage",
        "size",
        "stargazers_count",
        "watchers_count",
        "language",
        "has_issues",
        "has_downloads",
        "has_wiki",
        "forks_count",
        "mirror_url",
        "open_issues_count",
        "forks",
        "open_issues",
        "watchers",
        "default_branch",
        "master_branch"
})
public final class GithubData {

    @JsonProperty("id")
    private long id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("full_name")
    private String full_name;
    @JsonProperty("owner")
    private Owner owner;
    @JsonProperty("private")
    private boolean _private;
    @JsonProperty("html_url")
    private String html_url;
    @JsonProperty("description")
    private String description;
    @JsonProperty("fork")
    private boolean fork;
    @JsonProperty("url")
    private String url;
    @JsonProperty("forks_url")
    private String forks_url;
    @JsonProperty("keys_url")
    private String keys_url;
    @JsonProperty("collaborators_url")
    private String collaborators_url;
    @JsonProperty("teams_url")
    private String teams_url;
    @JsonProperty("hooks_url")
    private String hooks_url;
    @JsonProperty("issue_events_url")
    private String issue_events_url;
    @JsonProperty("events_url")
    private String events_url;
    @JsonProperty("assignees_url")
    private String assignees_url;
    @JsonProperty("branches_url")
    private String branches_url;
    @JsonProperty("tags_url")
    private String tags_url;
    @JsonProperty("blobs_url")
    private String blobs_url;
    @JsonProperty("git_tags_url")
    private String git_tags_url;
    @JsonProperty("git_refs_url")
    private String git_refs_url;
    @JsonProperty("trees_url")
    private String trees_url;
    @JsonProperty("statuses_url")
    private String statuses_url;
    @JsonProperty("languages_url")
    private String languages_url;
    @JsonProperty("stargazers_url")
    private String stargazers_url;
    @JsonProperty("contributors_url")
    private String contributors_url;
    @JsonProperty("subscribers_url")
    private String subscribers_url;
    @JsonProperty("subscription_url")
    private String subscription_url;
    @JsonProperty("commits_url")
    private String commits_url;
    @JsonProperty("git_commits_url")
    private String git_commits_url;
    @JsonProperty("comments_url")
    private String comments_url;
    @JsonProperty("issue_comment_url")
    private String issue_comment_url;
    @JsonProperty("contents_url")
    private String contents_url;
    @JsonProperty("compare_url")
    private String compare_url;
    @JsonProperty("merges_url")
    private String merges_url;
    @JsonProperty("archive_url")
    private String archive_url;
    @JsonProperty("downloads_url")
    private String downloads_url;
    @JsonProperty("issues_url")
    private String issues_url;
    @JsonProperty("pulls_url")
    private String pulls_url;
    @JsonProperty("milestones_url")
    private String milestones_url;
    @JsonProperty("notifications_url")
    private String notifications_url;
    @JsonProperty("labels_url")
    private String labels_url;
    @JsonProperty("releases_url")
    private String releases_url;
    @JsonProperty("created_at")
    private String created_at;
    @JsonProperty("updated_at")
    private String updated_at;
    @JsonProperty("pushed_at")
    private String pushed_at;
    @JsonProperty("git_url")
    private String git_url;
    @JsonProperty("ssh_url")
    private String ssh_url;
    @JsonProperty("clone_url")
    private String clone_url;
    @JsonProperty("svn_url")
    private String svn_url;
    @JsonProperty("homepage")
    private String homepage;
    @JsonProperty("size")
    private long size;
    @JsonProperty("stargazers_count")
    private long stargazers_count;
    @JsonProperty("watchers_count")
    private long watchers_count;
    @JsonProperty("language")
    private String language;
    @JsonProperty("has_issues")
    private boolean has_issues;
    @JsonProperty("has_downloads")
    private boolean has_downloads;
    @JsonProperty("has_wiki")
    private boolean has_wiki;
    @JsonProperty("forks_count")
    private long forks_count;
    @JsonProperty("mirror_url")
    private Object mirror_url;
    @JsonProperty("open_issues_count")
    private long open_issues_count;
    @JsonProperty("forks")
    private long forks;
    @JsonProperty("open_issues")
    private long open_issues;
    @JsonProperty("watchers")
    private long watchers;
    @JsonProperty("default_branch")
    private String default_branch;
    @JsonProperty("master_branch")
    private String master_branch;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("id")
    public long getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(long id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("full_name")
    public String getFull_name() {
        return full_name;
    }

    @JsonProperty("full_name")
    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    @JsonProperty("owner")
    public Owner getOwner() {
        return owner;
    }

    @JsonProperty("owner")
    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    @JsonProperty("private")
    public boolean isPrivate() {
        return _private;
    }

    @JsonProperty("private")
    public void setPrivate(boolean _private) {
        this._private = _private;
    }

    @JsonProperty("html_url")
    public String getHtml_url() {
        return html_url;
    }

    @JsonProperty("html_url")
    public void setHtml_url(String html_url) {
        this.html_url = html_url;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("fork")
    public boolean isFork() {
        return fork;
    }

    @JsonProperty("fork")
    public void setFork(boolean fork) {
        this.fork = fork;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonProperty("forks_url")
    public String getForks_url() {
        return forks_url;
    }

    @JsonProperty("forks_url")
    public void setForks_url(String forks_url) {
        this.forks_url = forks_url;
    }

    @JsonProperty("keys_url")
    public String getKeys_url() {
        return keys_url;
    }

    @JsonProperty("keys_url")
    public void setKeys_url(String keys_url) {
        this.keys_url = keys_url;
    }

    @JsonProperty("collaborators_url")
    public String getCollaborators_url() {
        return collaborators_url;
    }

    @JsonProperty("collaborators_url")
    public void setCollaborators_url(String collaborators_url) {
        this.collaborators_url = collaborators_url;
    }

    @JsonProperty("teams_url")
    public String getTeams_url() {
        return teams_url;
    }

    @JsonProperty("teams_url")
    public void setTeams_url(String teams_url) {
        this.teams_url = teams_url;
    }

    @JsonProperty("hooks_url")
    public String getHooks_url() {
        return hooks_url;
    }

    @JsonProperty("hooks_url")
    public void setHooks_url(String hooks_url) {
        this.hooks_url = hooks_url;
    }

    @JsonProperty("issue_events_url")
    public String getIssue_events_url() {
        return issue_events_url;
    }

    @JsonProperty("issue_events_url")
    public void setIssue_events_url(String issue_events_url) {
        this.issue_events_url = issue_events_url;
    }

    @JsonProperty("events_url")
    public String getEvents_url() {
        return events_url;
    }

    @JsonProperty("events_url")
    public void setEvents_url(String events_url) {
        this.events_url = events_url;
    }

    @JsonProperty("assignees_url")
    public String getAssignees_url() {
        return assignees_url;
    }

    @JsonProperty("assignees_url")
    public void setAssignees_url(String assignees_url) {
        this.assignees_url = assignees_url;
    }

    @JsonProperty("branches_url")
    public String getBranches_url() {
        return branches_url;
    }

    @JsonProperty("branches_url")
    public void setBranches_url(String branches_url) {
        this.branches_url = branches_url;
    }

    @JsonProperty("tags_url")
    public String getTags_url() {
        return tags_url;
    }

    @JsonProperty("tags_url")
    public void setTags_url(String tags_url) {
        this.tags_url = tags_url;
    }

    @JsonProperty("blobs_url")
    public String getBlobs_url() {
        return blobs_url;
    }

    @JsonProperty("blobs_url")
    public void setBlobs_url(String blobs_url) {
        this.blobs_url = blobs_url;
    }

    @JsonProperty("git_tags_url")
    public String getGit_tags_url() {
        return git_tags_url;
    }

    @JsonProperty("git_tags_url")
    public void setGit_tags_url(String git_tags_url) {
        this.git_tags_url = git_tags_url;
    }

    @JsonProperty("git_refs_url")
    public String getGit_refs_url() {
        return git_refs_url;
    }

    @JsonProperty("git_refs_url")
    public void setGit_refs_url(String git_refs_url) {
        this.git_refs_url = git_refs_url;
    }

    @JsonProperty("trees_url")
    public String getTrees_url() {
        return trees_url;
    }

    @JsonProperty("trees_url")
    public void setTrees_url(String trees_url) {
        this.trees_url = trees_url;
    }

    @JsonProperty("statuses_url")
    public String getStatuses_url() {
        return statuses_url;
    }

    @JsonProperty("statuses_url")
    public void setStatuses_url(String statuses_url) {
        this.statuses_url = statuses_url;
    }

    @JsonProperty("languages_url")
    public String getLanguages_url() {
        return languages_url;
    }

    @JsonProperty("languages_url")
    public void setLanguages_url(String languages_url) {
        this.languages_url = languages_url;
    }

    @JsonProperty("stargazers_url")
    public String getStargazers_url() {
        return stargazers_url;
    }

    @JsonProperty("stargazers_url")
    public void setStargazers_url(String stargazers_url) {
        this.stargazers_url = stargazers_url;
    }

    @JsonProperty("contributors_url")
    public String getContributors_url() {
        return contributors_url;
    }

    @JsonProperty("contributors_url")
    public void setContributors_url(String contributors_url) {
        this.contributors_url = contributors_url;
    }

    @JsonProperty("subscribers_url")
    public String getSubscribers_url() {
        return subscribers_url;
    }

    @JsonProperty("subscribers_url")
    public void setSubscribers_url(String subscribers_url) {
        this.subscribers_url = subscribers_url;
    }

    @JsonProperty("subscription_url")
    public String getSubscription_url() {
        return subscription_url;
    }

    @JsonProperty("subscription_url")
    public void setSubscription_url(String subscription_url) {
        this.subscription_url = subscription_url;
    }

    @JsonProperty("commits_url")
    public String getCommits_url() {
        return commits_url;
    }

    @JsonProperty("commits_url")
    public void setCommits_url(String commits_url) {
        this.commits_url = commits_url;
    }

    @JsonProperty("git_commits_url")
    public String getGit_commits_url() {
        return git_commits_url;
    }

    @JsonProperty("git_commits_url")
    public void setGit_commits_url(String git_commits_url) {
        this.git_commits_url = git_commits_url;
    }

    @JsonProperty("comments_url")
    public String getComments_url() {
        return comments_url;
    }

    @JsonProperty("comments_url")
    public void setComments_url(String comments_url) {
        this.comments_url = comments_url;
    }

    @JsonProperty("issue_comment_url")
    public String getIssue_comment_url() {
        return issue_comment_url;
    }

    @JsonProperty("issue_comment_url")
    public void setIssue_comment_url(String issue_comment_url) {
        this.issue_comment_url = issue_comment_url;
    }

    @JsonProperty("contents_url")
    public String getContents_url() {
        return contents_url;
    }

    @JsonProperty("contents_url")
    public void setContents_url(String contents_url) {
        this.contents_url = contents_url;
    }

    @JsonProperty("compare_url")
    public String getCompare_url() {
        return compare_url;
    }

    @JsonProperty("compare_url")
    public void setCompare_url(String compare_url) {
        this.compare_url = compare_url;
    }

    @JsonProperty("merges_url")
    public String getMerges_url() {
        return merges_url;
    }

    @JsonProperty("merges_url")
    public void setMerges_url(String merges_url) {
        this.merges_url = merges_url;
    }

    @JsonProperty("archive_url")
    public String getArchive_url() {
        return archive_url;
    }

    @JsonProperty("archive_url")
    public void setArchive_url(String archive_url) {
        this.archive_url = archive_url;
    }

    @JsonProperty("downloads_url")
    public String getDownloads_url() {
        return downloads_url;
    }

    @JsonProperty("downloads_url")
    public void setDownloads_url(String downloads_url) {
        this.downloads_url = downloads_url;
    }

    @JsonProperty("issues_url")
    public String getIssues_url() {
        return issues_url;
    }

    @JsonProperty("issues_url")
    public void setIssues_url(String issues_url) {
        this.issues_url = issues_url;
    }

    @JsonProperty("pulls_url")
    public String getPulls_url() {
        return pulls_url;
    }

    @JsonProperty("pulls_url")
    public void setPulls_url(String pulls_url) {
        this.pulls_url = pulls_url;
    }

    @JsonProperty("milestones_url")
    public String getMilestones_url() {
        return milestones_url;
    }

    @JsonProperty("milestones_url")
    public void setMilestones_url(String milestones_url) {
        this.milestones_url = milestones_url;
    }

    @JsonProperty("notifications_url")
    public String getNotifications_url() {
        return notifications_url;
    }

    @JsonProperty("notifications_url")
    public void setNotifications_url(String notifications_url) {
        this.notifications_url = notifications_url;
    }

    @JsonProperty("labels_url")
    public String getLabels_url() {
        return labels_url;
    }

    @JsonProperty("labels_url")
    public void setLabels_url(String labels_url) {
        this.labels_url = labels_url;
    }

    @JsonProperty("releases_url")
    public String getReleases_url() {
        return releases_url;
    }

    @JsonProperty("releases_url")
    public void setReleases_url(String releases_url) {
        this.releases_url = releases_url;
    }

    @JsonProperty("created_at")
    public String getCreated_at() {
        return created_at;
    }

    @JsonProperty("created_at")
    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    @JsonProperty("updated_at")
    public String getUpdated_at() {
        return updated_at;
    }

    @JsonProperty("updated_at")
    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    @JsonProperty("pushed_at")
    public String getPushed_at() {
        return pushed_at;
    }

    @JsonProperty("pushed_at")
    public void setPushed_at(String pushed_at) {
        this.pushed_at = pushed_at;
    }

    @JsonProperty("git_url")
    public String getGit_url() {
        return git_url;
    }

    @JsonProperty("git_url")
    public void setGit_url(String git_url) {
        this.git_url = git_url;
    }

    @JsonProperty("ssh_url")
    public String getSsh_url() {
        return ssh_url;
    }

    @JsonProperty("ssh_url")
    public void setSsh_url(String ssh_url) {
        this.ssh_url = ssh_url;
    }

    @JsonProperty("clone_url")
    public String getClone_url() {
        return clone_url;
    }

    @JsonProperty("clone_url")
    public void setClone_url(String clone_url) {
        this.clone_url = clone_url;
    }

    @JsonProperty("svn_url")
    public String getSvn_url() {
        return svn_url;
    }

    @JsonProperty("svn_url")
    public void setSvn_url(String svn_url) {
        this.svn_url = svn_url;
    }

    @JsonProperty("homepage")
    public String getHomepage() {
        return homepage;
    }

    @JsonProperty("homepage")
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    @JsonProperty("size")
    public long getSize() {
        return size;
    }

    @JsonProperty("size")
    public void setSize(long size) {
        this.size = size;
    }

    @JsonProperty("stargazers_count")
    public long getStargazers_count() {
        return stargazers_count;
    }

    @JsonProperty("stargazers_count")
    public void setStargazers_count(long stargazers_count) {
        this.stargazers_count = stargazers_count;
    }

    @JsonProperty("watchers_count")
    public long getWatchers_count() {
        return watchers_count;
    }

    @JsonProperty("watchers_count")
    public void setWatchers_count(long watchers_count) {
        this.watchers_count = watchers_count;
    }

    @JsonProperty("language")
    public String getLanguage() {
        return language;
    }

    @JsonProperty("language")
    public void setLanguage(String language) {
        this.language = language;
    }

    @JsonProperty("has_issues")
    public boolean isHas_issues() {
        return has_issues;
    }

    @JsonProperty("has_issues")
    public void setHas_issues(boolean has_issues) {
        this.has_issues = has_issues;
    }

    @JsonProperty("has_downloads")
    public boolean isHas_downloads() {
        return has_downloads;
    }

    @JsonProperty("has_downloads")
    public void setHas_downloads(boolean has_downloads) {
        this.has_downloads = has_downloads;
    }

    @JsonProperty("has_wiki")
    public boolean isHas_wiki() {
        return has_wiki;
    }

    @JsonProperty("has_wiki")
    public void setHas_wiki(boolean has_wiki) {
        this.has_wiki = has_wiki;
    }

    @JsonProperty("forks_count")
    public long getForks_count() {
        return forks_count;
    }

    @JsonProperty("forks_count")
    public void setForks_count(long forks_count) {
        this.forks_count = forks_count;
    }

    @JsonProperty("mirror_url")
    public Object getMirror_url() {
        return mirror_url;
    }

    @JsonProperty("mirror_url")
    public void setMirror_url(Object mirror_url) {
        this.mirror_url = mirror_url;
    }

    @JsonProperty("open_issues_count")
    public long getOpen_issues_count() {
        return open_issues_count;
    }

    @JsonProperty("open_issues_count")
    public void setOpen_issues_count(long open_issues_count) {
        this.open_issues_count = open_issues_count;
    }

    @JsonProperty("forks")
    public long getForks() {
        return forks;
    }

    @JsonProperty("forks")
    public void setForks(long forks) {
        this.forks = forks;
    }

    @JsonProperty("open_issues")
    public long getOpen_issues() {
        return open_issues;
    }

    @JsonProperty("open_issues")
    public void setOpen_issues(long open_issues) {
        this.open_issues = open_issues;
    }

    @JsonProperty("watchers")
    public long getWatchers() {
        return watchers;
    }

    @JsonProperty("watchers")
    public void setWatchers(long watchers) {
        this.watchers = watchers;
    }

    @JsonProperty("default_branch")
    public String getDefault_branch() {
        return default_branch;
    }

    @JsonProperty("default_branch")
    public void setDefault_branch(String default_branch) {
        this.default_branch = default_branch;
    }

    @JsonProperty("master_branch")
    public String getMaster_branch() {
        return master_branch;
    }

    @JsonProperty("master_branch")
    public void setMaster_branch(String master_branch) {
        this.master_branch = master_branch;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof GithubData)) return false;

        final GithubData that = (GithubData) o;

        if (_private != that._private) return false;
        if (fork != that.fork) return false;
        if (forks != that.forks) return false;
        if (forks_count != that.forks_count) return false;
        if (has_downloads != that.has_downloads) return false;
        if (has_issues != that.has_issues) return false;
        if (has_wiki != that.has_wiki) return false;
        if (id != that.id) return false;
        if (open_issues != that.open_issues) return false;
        if (open_issues_count != that.open_issues_count) return false;
        if (size != that.size) return false;
        if (stargazers_count != that.stargazers_count) return false;
        if (watchers != that.watchers) return false;
        if (watchers_count != that.watchers_count) return false;
        if (additionalProperties != null ? !additionalProperties.equals(that.additionalProperties) : that.additionalProperties != null)
            return false;
        if (archive_url != null ? !archive_url.equals(that.archive_url) : that.archive_url != null) return false;
        if (assignees_url != null ? !assignees_url.equals(that.assignees_url) : that.assignees_url != null)
            return false;
        if (blobs_url != null ? !blobs_url.equals(that.blobs_url) : that.blobs_url != null) return false;
        if (branches_url != null ? !branches_url.equals(that.branches_url) : that.branches_url != null) return false;
        if (clone_url != null ? !clone_url.equals(that.clone_url) : that.clone_url != null) return false;
        if (collaborators_url != null ? !collaborators_url.equals(that.collaborators_url) : that.collaborators_url != null)
            return false;
        if (comments_url != null ? !comments_url.equals(that.comments_url) : that.comments_url != null) return false;
        if (commits_url != null ? !commits_url.equals(that.commits_url) : that.commits_url != null) return false;
        if (compare_url != null ? !compare_url.equals(that.compare_url) : that.compare_url != null) return false;
        if (contents_url != null ? !contents_url.equals(that.contents_url) : that.contents_url != null) return false;
        if (contributors_url != null ? !contributors_url.equals(that.contributors_url) : that.contributors_url != null)
            return false;
        if (created_at != null ? !created_at.equals(that.created_at) : that.created_at != null) return false;
        if (default_branch != null ? !default_branch.equals(that.default_branch) : that.default_branch != null)
            return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (downloads_url != null ? !downloads_url.equals(that.downloads_url) : that.downloads_url != null)
            return false;
        if (events_url != null ? !events_url.equals(that.events_url) : that.events_url != null) return false;
        if (forks_url != null ? !forks_url.equals(that.forks_url) : that.forks_url != null) return false;
        if (full_name != null ? !full_name.equals(that.full_name) : that.full_name != null) return false;
        if (git_commits_url != null ? !git_commits_url.equals(that.git_commits_url) : that.git_commits_url != null)
            return false;
        if (git_refs_url != null ? !git_refs_url.equals(that.git_refs_url) : that.git_refs_url != null) return false;
        if (git_tags_url != null ? !git_tags_url.equals(that.git_tags_url) : that.git_tags_url != null) return false;
        if (git_url != null ? !git_url.equals(that.git_url) : that.git_url != null) return false;
        if (homepage != null ? !homepage.equals(that.homepage) : that.homepage != null) return false;
        if (hooks_url != null ? !hooks_url.equals(that.hooks_url) : that.hooks_url != null) return false;
        if (html_url != null ? !html_url.equals(that.html_url) : that.html_url != null) return false;
        if (issue_comment_url != null ? !issue_comment_url.equals(that.issue_comment_url) : that.issue_comment_url != null)
            return false;
        if (issue_events_url != null ? !issue_events_url.equals(that.issue_events_url) : that.issue_events_url != null)
            return false;
        if (issues_url != null ? !issues_url.equals(that.issues_url) : that.issues_url != null) return false;
        if (keys_url != null ? !keys_url.equals(that.keys_url) : that.keys_url != null) return false;
        if (labels_url != null ? !labels_url.equals(that.labels_url) : that.labels_url != null) return false;
        if (language != null ? !language.equals(that.language) : that.language != null) return false;
        if (languages_url != null ? !languages_url.equals(that.languages_url) : that.languages_url != null)
            return false;
        if (master_branch != null ? !master_branch.equals(that.master_branch) : that.master_branch != null)
            return false;
        if (merges_url != null ? !merges_url.equals(that.merges_url) : that.merges_url != null) return false;
        if (milestones_url != null ? !milestones_url.equals(that.milestones_url) : that.milestones_url != null)
            return false;
        if (mirror_url != null ? !mirror_url.equals(that.mirror_url) : that.mirror_url != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (notifications_url != null ? !notifications_url.equals(that.notifications_url) : that.notifications_url != null)
            return false;
        if (owner != null ? !owner.equals(that.owner) : that.owner != null) return false;
        if (pulls_url != null ? !pulls_url.equals(that.pulls_url) : that.pulls_url != null) return false;
        if (pushed_at != null ? !pushed_at.equals(that.pushed_at) : that.pushed_at != null) return false;
        if (releases_url != null ? !releases_url.equals(that.releases_url) : that.releases_url != null) return false;
        if (ssh_url != null ? !ssh_url.equals(that.ssh_url) : that.ssh_url != null) return false;
        if (stargazers_url != null ? !stargazers_url.equals(that.stargazers_url) : that.stargazers_url != null)
            return false;
        if (statuses_url != null ? !statuses_url.equals(that.statuses_url) : that.statuses_url != null) return false;
        if (subscribers_url != null ? !subscribers_url.equals(that.subscribers_url) : that.subscribers_url != null)
            return false;
        if (subscription_url != null ? !subscription_url.equals(that.subscription_url) : that.subscription_url != null)
            return false;
        if (svn_url != null ? !svn_url.equals(that.svn_url) : that.svn_url != null) return false;
        if (tags_url != null ? !tags_url.equals(that.tags_url) : that.tags_url != null) return false;
        if (teams_url != null ? !teams_url.equals(that.teams_url) : that.teams_url != null) return false;
        if (trees_url != null ? !trees_url.equals(that.trees_url) : that.trees_url != null) return false;
        if (updated_at != null ? !updated_at.equals(that.updated_at) : that.updated_at != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (full_name != null ? full_name.hashCode() : 0);
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (_private ? 1 : 0);
        result = 31 * result + (html_url != null ? html_url.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (fork ? 1 : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (forks_url != null ? forks_url.hashCode() : 0);
        result = 31 * result + (keys_url != null ? keys_url.hashCode() : 0);
        result = 31 * result + (collaborators_url != null ? collaborators_url.hashCode() : 0);
        result = 31 * result + (teams_url != null ? teams_url.hashCode() : 0);
        result = 31 * result + (hooks_url != null ? hooks_url.hashCode() : 0);
        result = 31 * result + (issue_events_url != null ? issue_events_url.hashCode() : 0);
        result = 31 * result + (events_url != null ? events_url.hashCode() : 0);
        result = 31 * result + (assignees_url != null ? assignees_url.hashCode() : 0);
        result = 31 * result + (branches_url != null ? branches_url.hashCode() : 0);
        result = 31 * result + (tags_url != null ? tags_url.hashCode() : 0);
        result = 31 * result + (blobs_url != null ? blobs_url.hashCode() : 0);
        result = 31 * result + (git_tags_url != null ? git_tags_url.hashCode() : 0);
        result = 31 * result + (git_refs_url != null ? git_refs_url.hashCode() : 0);
        result = 31 * result + (trees_url != null ? trees_url.hashCode() : 0);
        result = 31 * result + (statuses_url != null ? statuses_url.hashCode() : 0);
        result = 31 * result + (languages_url != null ? languages_url.hashCode() : 0);
        result = 31 * result + (stargazers_url != null ? stargazers_url.hashCode() : 0);
        result = 31 * result + (contributors_url != null ? contributors_url.hashCode() : 0);
        result = 31 * result + (subscribers_url != null ? subscribers_url.hashCode() : 0);
        result = 31 * result + (subscription_url != null ? subscription_url.hashCode() : 0);
        result = 31 * result + (commits_url != null ? commits_url.hashCode() : 0);
        result = 31 * result + (git_commits_url != null ? git_commits_url.hashCode() : 0);
        result = 31 * result + (comments_url != null ? comments_url.hashCode() : 0);
        result = 31 * result + (issue_comment_url != null ? issue_comment_url.hashCode() : 0);
        result = 31 * result + (contents_url != null ? contents_url.hashCode() : 0);
        result = 31 * result + (compare_url != null ? compare_url.hashCode() : 0);
        result = 31 * result + (merges_url != null ? merges_url.hashCode() : 0);
        result = 31 * result + (archive_url != null ? archive_url.hashCode() : 0);
        result = 31 * result + (downloads_url != null ? downloads_url.hashCode() : 0);
        result = 31 * result + (issues_url != null ? issues_url.hashCode() : 0);
        result = 31 * result + (pulls_url != null ? pulls_url.hashCode() : 0);
        result = 31 * result + (milestones_url != null ? milestones_url.hashCode() : 0);
        result = 31 * result + (notifications_url != null ? notifications_url.hashCode() : 0);
        result = 31 * result + (labels_url != null ? labels_url.hashCode() : 0);
        result = 31 * result + (releases_url != null ? releases_url.hashCode() : 0);
        result = 31 * result + (created_at != null ? created_at.hashCode() : 0);
        result = 31 * result + (updated_at != null ? updated_at.hashCode() : 0);
        result = 31 * result + (pushed_at != null ? pushed_at.hashCode() : 0);
        result = 31 * result + (git_url != null ? git_url.hashCode() : 0);
        result = 31 * result + (ssh_url != null ? ssh_url.hashCode() : 0);
        result = 31 * result + (clone_url != null ? clone_url.hashCode() : 0);
        result = 31 * result + (svn_url != null ? svn_url.hashCode() : 0);
        result = 31 * result + (homepage != null ? homepage.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (stargazers_count ^ (stargazers_count >>> 32));
        result = 31 * result + (int) (watchers_count ^ (watchers_count >>> 32));
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (has_issues ? 1 : 0);
        result = 31 * result + (has_downloads ? 1 : 0);
        result = 31 * result + (has_wiki ? 1 : 0);
        result = 31 * result + (int) (forks_count ^ (forks_count >>> 32));
        result = 31 * result + (mirror_url != null ? mirror_url.hashCode() : 0);
        result = 31 * result + (int) (open_issues_count ^ (open_issues_count >>> 32));
        result = 31 * result + (int) (forks ^ (forks >>> 32));
        result = 31 * result + (int) (open_issues ^ (open_issues >>> 32));
        result = 31 * result + (int) (watchers ^ (watchers >>> 32));
        result = 31 * result + (default_branch != null ? default_branch.hashCode() : 0);
        result = 31 * result + (master_branch != null ? master_branch.hashCode() : 0);
        result = 31 * result + (additionalProperties != null ? additionalProperties.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GithubData{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", full_name='").append(full_name).append('\'');
        sb.append(", owner=").append(owner);
        sb.append(", _private=").append(_private);
        sb.append(", html_url='").append(html_url).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", fork=").append(fork);
        sb.append(", url='").append(url).append('\'');
        sb.append(", forks_url='").append(forks_url).append('\'');
        sb.append(", keys_url='").append(keys_url).append('\'');
        sb.append(", collaborators_url='").append(collaborators_url).append('\'');
        sb.append(", teams_url='").append(teams_url).append('\'');
        sb.append(", hooks_url='").append(hooks_url).append('\'');
        sb.append(", issue_events_url='").append(issue_events_url).append('\'');
        sb.append(", events_url='").append(events_url).append('\'');
        sb.append(", assignees_url='").append(assignees_url).append('\'');
        sb.append(", branches_url='").append(branches_url).append('\'');
        sb.append(", tags_url='").append(tags_url).append('\'');
        sb.append(", blobs_url='").append(blobs_url).append('\'');
        sb.append(", git_tags_url='").append(git_tags_url).append('\'');
        sb.append(", git_refs_url='").append(git_refs_url).append('\'');
        sb.append(", trees_url='").append(trees_url).append('\'');
        sb.append(", statuses_url='").append(statuses_url).append('\'');
        sb.append(", languages_url='").append(languages_url).append('\'');
        sb.append(", stargazers_url='").append(stargazers_url).append('\'');
        sb.append(", contributors_url='").append(contributors_url).append('\'');
        sb.append(", subscribers_url='").append(subscribers_url).append('\'');
        sb.append(", subscription_url='").append(subscription_url).append('\'');
        sb.append(", commits_url='").append(commits_url).append('\'');
        sb.append(", git_commits_url='").append(git_commits_url).append('\'');
        sb.append(", comments_url='").append(comments_url).append('\'');
        sb.append(", issue_comment_url='").append(issue_comment_url).append('\'');
        sb.append(", contents_url='").append(contents_url).append('\'');
        sb.append(", compare_url='").append(compare_url).append('\'');
        sb.append(", merges_url='").append(merges_url).append('\'');
        sb.append(", archive_url='").append(archive_url).append('\'');
        sb.append(", downloads_url='").append(downloads_url).append('\'');
        sb.append(", issues_url='").append(issues_url).append('\'');
        sb.append(", pulls_url='").append(pulls_url).append('\'');
        sb.append(", milestones_url='").append(milestones_url).append('\'');
        sb.append(", notifications_url='").append(notifications_url).append('\'');
        sb.append(", labels_url='").append(labels_url).append('\'');
        sb.append(", releases_url='").append(releases_url).append('\'');
        sb.append(", created_at='").append(created_at).append('\'');
        sb.append(", updated_at='").append(updated_at).append('\'');
        sb.append(", pushed_at='").append(pushed_at).append('\'');
        sb.append(", git_url='").append(git_url).append('\'');
        sb.append(", ssh_url='").append(ssh_url).append('\'');
        sb.append(", clone_url='").append(clone_url).append('\'');
        sb.append(", svn_url='").append(svn_url).append('\'');
        sb.append(", homepage='").append(homepage).append('\'');
        sb.append(", size=").append(size);
        sb.append(", stargazers_count=").append(stargazers_count);
        sb.append(", watchers_count=").append(watchers_count);
        sb.append(", language='").append(language).append('\'');
        sb.append(", has_issues=").append(has_issues);
        sb.append(", has_downloads=").append(has_downloads);
        sb.append(", has_wiki=").append(has_wiki);
        sb.append(", forks_count=").append(forks_count);
        sb.append(", mirror_url=").append(mirror_url);
        sb.append(", open_issues_count=").append(open_issues_count);
        sb.append(", forks=").append(forks);
        sb.append(", open_issues=").append(open_issues);
        sb.append(", watchers=").append(watchers);
        sb.append(", default_branch='").append(default_branch).append('\'');
        sb.append(", master_branch='").append(master_branch).append('\'');
        sb.append(", additionalProperties=").append(additionalProperties);
        sb.append('}');
        return sb.toString();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Generated("com.googlecode.jsonschema2pojo")
    @JsonPropertyOrder({
            "login",
            "id",
            "avatar_url",
            "gravatar_id",
            "url",
            "html_url",
            "followers_url",
            "following_url",
            "gists_url",
            "starred_url",
            "subscriptions_url",
            "organizations_url",
            "repos_url",
            "events_url",
            "received_events_url",
            "type",
            "site_admin"
    })
    public static final class Owner {

        @JsonProperty("login")
        private String login;
        @JsonProperty("id")
        private long id;
        @JsonProperty("avatar_url")
        private String avatar_url;
        @JsonProperty("gravatar_id")
        private String gravatar_id;
        @JsonProperty("url")
        private String url;
        @JsonProperty("html_url")
        private String html_url;
        @JsonProperty("followers_url")
        private String followers_url;
        @JsonProperty("following_url")
        private String following_url;
        @JsonProperty("gists_url")
        private String gists_url;
        @JsonProperty("starred_url")
        private String starred_url;
        @JsonProperty("subscriptions_url")
        private String subscriptions_url;
        @JsonProperty("organizations_url")
        private String organizations_url;
        @JsonProperty("repos_url")
        private String repos_url;
        @JsonProperty("events_url")
        private String events_url;
        @JsonProperty("received_events_url")
        private String received_events_url;
        @JsonProperty("type")
        private String type;
        @JsonProperty("site_admin")
        private boolean site_admin;
        private Map<String, Object> additionalProperties = new HashMap<String, Object>();

        @JsonProperty("login")
        public String getLogin() {
            return login;
        }

        @JsonProperty("login")
        public void setLogin(String login) {
            this.login = login;
        }

        @JsonProperty("id")
        public long getId() {
            return id;
        }

        @JsonProperty("id")
        public void setId(long id) {
            this.id = id;
        }

        @JsonProperty("avatar_url")
        public String getAvatar_url() {
            return avatar_url;
        }

        @JsonProperty("avatar_url")
        public void setAvatar_url(String avatar_url) {
            this.avatar_url = avatar_url;
        }

        @JsonProperty("gravatar_id")
        public String getGravatar_id() {
            return gravatar_id;
        }

        @JsonProperty("gravatar_id")
        public void setGravatar_id(String gravatar_id) {
            this.gravatar_id = gravatar_id;
        }

        @JsonProperty("url")
        public String getUrl() {
            return url;
        }

        @JsonProperty("url")
        public void setUrl(String url) {
            this.url = url;
        }

        @JsonProperty("html_url")
        public String getHtml_url() {
            return html_url;
        }

        @JsonProperty("html_url")
        public void setHtml_url(String html_url) {
            this.html_url = html_url;
        }

        @JsonProperty("followers_url")
        public String getFollowers_url() {
            return followers_url;
        }

        @JsonProperty("followers_url")
        public void setFollowers_url(String followers_url) {
            this.followers_url = followers_url;
        }

        @JsonProperty("following_url")
        public String getFollowing_url() {
            return following_url;
        }

        @JsonProperty("following_url")
        public void setFollowing_url(String following_url) {
            this.following_url = following_url;
        }

        @JsonProperty("gists_url")
        public String getGists_url() {
            return gists_url;
        }

        @JsonProperty("gists_url")
        public void setGists_url(String gists_url) {
            this.gists_url = gists_url;
        }

        @JsonProperty("starred_url")
        public String getStarred_url() {
            return starred_url;
        }

        @JsonProperty("starred_url")
        public void setStarred_url(String starred_url) {
            this.starred_url = starred_url;
        }

        @JsonProperty("subscriptions_url")
        public String getSubscriptions_url() {
            return subscriptions_url;
        }

        @JsonProperty("subscriptions_url")
        public void setSubscriptions_url(String subscriptions_url) {
            this.subscriptions_url = subscriptions_url;
        }

        @JsonProperty("organizations_url")
        public String getOrganizations_url() {
            return organizations_url;
        }

        @JsonProperty("organizations_url")
        public void setOrganizations_url(String organizations_url) {
            this.organizations_url = organizations_url;
        }

        @JsonProperty("repos_url")
        public String getRepos_url() {
            return repos_url;
        }

        @JsonProperty("repos_url")
        public void setRepos_url(String repos_url) {
            this.repos_url = repos_url;
        }

        @JsonProperty("events_url")
        public String getEvents_url() {
            return events_url;
        }

        @JsonProperty("events_url")
        public void setEvents_url(String events_url) {
            this.events_url = events_url;
        }

        @JsonProperty("received_events_url")
        public String getReceived_events_url() {
            return received_events_url;
        }

        @JsonProperty("received_events_url")
        public void setReceived_events_url(String received_events_url) {
            this.received_events_url = received_events_url;
        }

        @JsonProperty("type")
        public String getType() {
            return type;
        }

        @JsonProperty("type")
        public void setType(String type) {
            this.type = type;
        }

        @JsonProperty("site_admin")
        public boolean isSite_admin() {
            return site_admin;
        }

        @JsonProperty("site_admin")
        public void setSite_admin(boolean site_admin) {
            this.site_admin = site_admin;
        }

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperties(String name, Object value) {
            this.additionalProperties.put(name, value);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof Owner)) return false;

            final Owner owner = (Owner) o;

            if (id != owner.id) return false;
            if (site_admin != owner.site_admin) return false;
            if (additionalProperties != null ? !additionalProperties.equals(owner.additionalProperties) : owner.additionalProperties != null)
                return false;
            if (avatar_url != null ? !avatar_url.equals(owner.avatar_url) : owner.avatar_url != null) return false;
            if (events_url != null ? !events_url.equals(owner.events_url) : owner.events_url != null) return false;
            if (followers_url != null ? !followers_url.equals(owner.followers_url) : owner.followers_url != null)
                return false;
            if (following_url != null ? !following_url.equals(owner.following_url) : owner.following_url != null)
                return false;
            if (gists_url != null ? !gists_url.equals(owner.gists_url) : owner.gists_url != null) return false;
            if (gravatar_id != null ? !gravatar_id.equals(owner.gravatar_id) : owner.gravatar_id != null) return false;
            if (html_url != null ? !html_url.equals(owner.html_url) : owner.html_url != null) return false;
            if (login != null ? !login.equals(owner.login) : owner.login != null) return false;
            if (organizations_url != null ? !organizations_url.equals(owner.organizations_url) : owner.organizations_url != null)
                return false;
            if (received_events_url != null ? !received_events_url.equals(owner.received_events_url) : owner.received_events_url != null)
                return false;
            if (repos_url != null ? !repos_url.equals(owner.repos_url) : owner.repos_url != null) return false;
            if (starred_url != null ? !starred_url.equals(owner.starred_url) : owner.starred_url != null) return false;
            if (subscriptions_url != null ? !subscriptions_url.equals(owner.subscriptions_url) : owner.subscriptions_url != null)
                return false;
            if (type != null ? !type.equals(owner.type) : owner.type != null) return false;
            if (url != null ? !url.equals(owner.url) : owner.url != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = login != null ? login.hashCode() : 0;
            result = 31 * result + (int) (id ^ (id >>> 32));
            result = 31 * result + (avatar_url != null ? avatar_url.hashCode() : 0);
            result = 31 * result + (gravatar_id != null ? gravatar_id.hashCode() : 0);
            result = 31 * result + (url != null ? url.hashCode() : 0);
            result = 31 * result + (html_url != null ? html_url.hashCode() : 0);
            result = 31 * result + (followers_url != null ? followers_url.hashCode() : 0);
            result = 31 * result + (following_url != null ? following_url.hashCode() : 0);
            result = 31 * result + (gists_url != null ? gists_url.hashCode() : 0);
            result = 31 * result + (starred_url != null ? starred_url.hashCode() : 0);
            result = 31 * result + (subscriptions_url != null ? subscriptions_url.hashCode() : 0);
            result = 31 * result + (organizations_url != null ? organizations_url.hashCode() : 0);
            result = 31 * result + (repos_url != null ? repos_url.hashCode() : 0);
            result = 31 * result + (events_url != null ? events_url.hashCode() : 0);
            result = 31 * result + (received_events_url != null ? received_events_url.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (site_admin ? 1 : 0);
            result = 31 * result + (additionalProperties != null ? additionalProperties.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Owner{");
            sb.append("login='").append(login).append('\'');
            sb.append(", id=").append(id);
            sb.append(", avatar_url='").append(avatar_url).append('\'');
            sb.append(", gravatar_id='").append(gravatar_id).append('\'');
            sb.append(", url='").append(url).append('\'');
            sb.append(", html_url='").append(html_url).append('\'');
            sb.append(", followers_url='").append(followers_url).append('\'');
            sb.append(", following_url='").append(following_url).append('\'');
            sb.append(", gists_url='").append(gists_url).append('\'');
            sb.append(", starred_url='").append(starred_url).append('\'');
            sb.append(", subscriptions_url='").append(subscriptions_url).append('\'');
            sb.append(", organizations_url='").append(organizations_url).append('\'');
            sb.append(", repos_url='").append(repos_url).append('\'');
            sb.append(", events_url='").append(events_url).append('\'');
            sb.append(", received_events_url='").append(received_events_url).append('\'');
            sb.append(", type='").append(type).append('\'');
            sb.append(", site_admin=").append(site_admin);
            sb.append(", additionalProperties=").append(additionalProperties);
            sb.append('}');
            return sb.toString();
        }
    }
}

