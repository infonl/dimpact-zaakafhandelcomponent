# Reducing Repository Size

This document describes how to analyze and reduce the size of the ZAC Git repository.

## Analyzing repository size

Most of the repository size is typically in Git history (`.git/objects`), not the working directory.

```shell
# Total repository size
du -sh .

# Git objects size (this is usually the largest)
du -sh .git

# Find largest blobs in history
git rev-list --objects --all | \
  git cat-file --batch-check='%(objectname) %(objecttype) %(objectsize) %(rest)' | \
  grep blob | sort -k3 -rn | head -20
```

## Squashing gh-pages history

The `gh-pages` branch is the main source of repository bloat. It stores e2e test videos (`.webm` files) that are regenerated with each test run. Over time, this accumulates gigabytes of old test recordings in Git history that are never needed again.

Squashing the gh-pages branch to a single commit while keeping only the latest content provides the largest space savings.

> ⚠️ **Warning**: This rewrites Git history. Coordinate with the team before proceeding.

1. Fetch the latest gh-pages:
    ```shell
    git fetch origin gh-pages
    git checkout gh-pages
    ```

2. Create an orphan branch with the same content (no history):
    ```shell
    git checkout --orphan gh-pages-clean
    git add -A
    git commit -m "chore: squash gh-pages history to reduce repository size"
    ```

3. Replace the old branch:
    ```shell
    git branch -D gh-pages
    git branch -m gh-pages
    git push origin gh-pages --force
    ```

4. Run garbage collection on the server. On GitHub, contact support for large repositories or wait for automatic garbage collection.

5. All team members should re-clone or run:
    ```shell
    git fetch origin gh-pages
    git branch -D gh-pages
    git checkout -b gh-pages origin/gh-pages
    git gc --aggressive --prune=now
    ```

### Preventing future bloat

Consider modifying the e2e publishing workflow to:
- Keep only the latest N test reports instead of all historical ones
- Store videos externally (e.g., in GitHub Releases or object storage) instead of in Git

## Additional optimizations

The following optimizations provide smaller but still meaningful space savings.

### Removing large binary files from history

Use `git-filter-repo` to remove large files from Git history. Install it first:

```shell
# Ubuntu/Debian
apt-get install git-filter-repo

# macOS
brew install git-filter-repo

# Or via pip
pip install git-filter-repo
```

Common space consumers:
- `package-lock.json` files (many versions in history)
- Binary files like JARs (e.g., `scripts/wildfly/galleon/bin/galleon-cli.jar`)
- PDF manuals in `docs/manuals/`
- Helm `index.yaml` (many versions)

To remove specific files:

1. Create a fresh mirror clone:
    ```shell
    git clone --mirror git@github.com:dimpact/dimpact-zaakafhandelcomponent.git zac-cleanup
    cd zac-cleanup
    ```

   > By default, `git filter-repo` rewrites **all branches and tags**. Use `--refs` to limit scope if needed.

2. Remove specific files from history:
    ```shell
    # Remove a specific file from ALL branches and tags
    git filter-repo --path scripts/wildfly/galleon/bin/galleon-cli.jar --invert-paths

    # Or limit to specific branches only:
    git filter-repo --path scripts/wildfly/galleon/bin/galleon-cli.jar --invert-paths \
      --refs refs/heads/main refs/heads/gh-pages

    # Remove files matching a pattern (e.g., versioned PDFs)
    git filter-repo --path-regex 'docs/manuals/.*V[0-9]+\.[0-9]+.*\.pdf' --invert-paths
    ```

3. Force push and have all team members re-clone:
    ```shell
    git push origin --force --all
    git push origin --force --tags
    ```

### Setting up Git LFS for binary files

Configure Git LFS to prevent binary files from bloating the repository in the future:

```shell
# Install
apt-get install git-lfs  # or: brew install git-lfs
git lfs install

# Track binary file types
git lfs track "*.jar"
git lfs track "*.pdf"

# Commit configuration
git add .gitattributes
git commit -m "chore: configure Git LFS for binary files"
```

### Deleting stale branches

Regularly clean up merged branches:

```shell
# List merged branches
git branch -r --merged origin/main | grep -v main | grep -v gh-pages

# Delete a single branch
git push origin --delete feature/old-branch-name

# Delete all merged branches (review the list first!)
git branch -r --merged origin/main | \
  grep -v main | \
  grep -v gh-pages | \
  sed 's/origin\///' | \
  xargs -I {} git push origin --delete {}
```

In GitHub, go to **Settings → General → Pull Requests** and enable **"Automatically delete head branches"**.

### Running garbage collection

After any cleanup, run garbage collection:

```shell
git gc --aggressive --prune=now
du -sh .git
```

## See also

- [git-filter-repo documentation](https://github.com/newren/git-filter-repo)
- [Git LFS documentation](https://git-lfs.github.com/)
- [GitHub repository size limits](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github)
