First off, thank you for considering contributing to JBeret project. It's people like you that make JBeret such a great library. JBeret is an open source project and contributions from our community are more than welcome! There are many ways to contribute, from writing tutorials or blog posts, improving the documentation, submitting bug reports and feature requests or writing code which can be incorporated into JBeret itself.

We expect all contributors and users to follow our [Code of Conduct](CODE_OF_CONDUCT.md) when communicating through project channels. These include, but are not limited to: chat, issues, code.

All recent development happens in the branch `main`. There are additional [branches](https://github.com/jberet/jsr352/branches) mainly used for maintenance.

# One time setup

## Create a GitHub account

If you don't have one already, head to https://github.com/

## Fork JBeret

Fork https://github.com/jberet/jsr352.git into your GitHub account.

## Clone your newly forked repository onto your local machine

```bash
git clone https://github.com/YOUR_NAME/jsr352.git
cd jsr352
```

## Add a remote reference to upstream

This makes it easy to pull down changes in the project over time

```bash
git remote add upstream https://github.com/jberet/jsr352.git
```

# Development Process

This is the typical process you would follow to submit any changes to JBeret.

## Pulling updates from upstream

```bash
git pull --rebase upstream main
```

> Note that --rebase will automatically move your local commits, if you have
> any, on top of the latest branch you pull from.
> If you don't have any commits it is safe to leave off, but for safety it
> doesn't hurt to use it each time just in case you have a commit you've
> forgotten about!

## Discuss your planned changes (if you want feedback)

 * JBeret JIRA issues - https://issues.redhat.com/projects/JBERET/issues/
 * JBeret github issus - https://github.com/jberet/jsr352/issues
 * JBeret github discussions - https://github.com/orgs/jberet/discussions
 * Zulip - https://wildfly.zulipchat.com/#narrow/stream/174184-wildfly-developers

## Create a simple topic branch to isolate your work (recommended)

```bash
git checkout -b my_cool_feature
```

## Make the changes

Make whatever code changes, including new tests to verify your change, are necessary and ensure that the build and tests pass.

```bash
mvn clean install
```

> If you're making non code changes, the above step is not required.

## Commit changes

Add whichever files were changed into 'staging' before performing a commit:

```bash
git commit
```

## Rebase changes against main

Once all your commits for the issue have been made against your local topic branch, we need to rebase it against branch main in upstream to ensure that your commits are added on top of the current state of main. This will make it easier to incorporate your changes into the main branch, especially if there has been any significant time passed since you rebased at the beginning.

```bash
git pull --rebase upstream main
```

## Push to your repo

Now that you've synced your topic branch with upstream, it's time to push it to your GitHub repo.

```bash
git push origin my_cool_feature
```

## Getting your changes merged into upstream, a pull request

Now your updates are in your GitHub repo, you will need to notify the project that you have code/docs for inclusion.

 * Send a pull request, by clicking the pull request link while in your repository fork
 * After review a maintainer will merge your pull request, update/resolve associated issues, and reply when complete
 * Lastly, switch back to branch main from your topic branch and pull the updates

```bash
git checkout main
git pull upstream main
```

 * You may also choose to update your origin on GitHub as well

```bash
git push origin
```

## Some tips

Here are some tips on increasing the chance that your pull request is accepted:

 * Write a [good commit message](https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)
 * Include tests that fail without your code, and pass with it

# Issues

JBeret uses JIRA and github issues to manage issues.
 * JBeret JIRA issues - https://issues.redhat.com/projects/JBERET/issues/
 * JBeret github issus - https://github.com/jberet/jsr352/issues

## Good First Issues

Want to contribute to JBeret but aren't quite sure where to start? Check out our issues with the `good first issue` label. These are a triaged set of issues that are great for getting started on our project. These can be found [here](https://github.com/jberet/jsr352/labels/good%20first%20issue).

Once you have selected an issue you'd like to work on, make sure it's not already assigned to someone else. To assign an issue to yourself, simply click on "Start Progress". This will automatically assign the issue to you.

