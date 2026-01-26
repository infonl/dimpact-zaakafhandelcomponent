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

The `gh-pages` branch is the main source of repository bloat. It stores e2e test videos (`.webm` files) that are generated on each test run. Over time, this accumulates gigabytes of old test recordings in Git history.

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

4. GitHub will perform automatic garbage collection eventually. Note that the space savings may not be immediately visible and can take several hours or days to fully reflect in the repository size.

5. All team members should re-clone or run:
    ```shell
    git fetch origin gh-pages
    git checkout main  # or any non-gh-pages branch
    git branch -D gh-pages
    git checkout -b gh-pages origin/gh-pages
    git gc --aggressive --prune=now
    ```

## See also

- [git-filter-repo documentation](https://github.com/newren/git-filter-repo)
- [Git LFS documentation](https://git-lfs.github.com/)
- [GitHub repository size limits](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github)
