/**
 *
 */
package org.theseed.config.git;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.theseed.io.LineReader;

/**
 * This object provides a simple API for manipulating a GIT repo. The repo is
 * constructed with a file location and it is presumed the remote origin is
 * already configured.
 *
 * @author Bruce Parrello
 *
 */
public class GitRepo implements AutoCloseable {

	// FIELDS
	/** logging facility */
	protected static Logger log = LoggerFactory.getLogger(GitRepo.class);
	/** target local repo */
	private Repository localRepo;
	/** JGit API object for the local repo */
	private Git repoGit;
	/** base name of this repo */
	private String baseName;

	/**
	 * Construct a GIT repo manager for a local file location.
	 *
	 * @param loc	file location of the repo
	 *
	 * @throws IOException
	 */
	public GitRepo(File loc) throws IOException {
		File gitLoc = new File(loc, ".git");
		// Compute the GIT directory.
		if (! gitLoc.isDirectory()) {
			// Here the GIT directory is not in the normal place, so we need to read
			// the directory name.
			if (! gitLoc.canRead())
				throw new IOException(loc + " does not have a readable GIT configuration.");
			try (LineReader gitStream = new LineReader(gitLoc)) {
				gitLoc = null;
				while (gitLoc == null && gitStream.hasNext()) {
					String line = gitStream.next();
					if (line.startsWith("gitdir:")) {
						String dirName = StringUtils.trimToEmpty(StringUtils.substringAfter(line, ":"));
						if (! StringUtils.isBlank(dirName)) {
							// Finally we have found a directory name. Save it as the git location.
							gitLoc = new File(dirName);
							if (! gitLoc.isAbsolute()) {
								// We have a relative path, so we need to canonicalize it.
								gitLoc = new File(loc, dirName);
								String canonPath = gitLoc.getCanonicalPath();
								gitLoc = new File(canonPath);
							}
						}
					}
				}
				if (gitLoc == null)
					throw new IOException("Could not find a GIT directory pointer for " + loc + ".");
			}
		}
		this.localRepo = new FileRepository(gitLoc);
		this.repoGit = new Git(this.localRepo);
		this.baseName = loc.getName();
	}

	/**
	 * Pull the latest version of the top-level repo.
	 *
	 * @param remote	remote tag to use (usually "origin")
	 * @param branch	branch to fetch (or NULL for the default)
	 *
	 * @return a merge result, indicating what updates occurred
	 *
	 * @throws GitAPIException
	 */
	public PullResult pull(String remote, String branch) throws GitAPIException {
		PullCommand cmd = this.repoGit.pull();
		cmd.setRemote(remote);
		if (branch == null) {
			// Get the current remote branch.
			branch = this.getBranch(remote);
		}
		cmd.setRemoteBranchName(branch);
		log.info("Pulling remote {} for module {}.", remote, this.baseName);
		PullResult retVal = cmd.call();
		return retVal;
	}

	/**
	 * Pull the latest version of the repo and all its submodules (if any).
	 *
	 * @param remote	remote tag to use (usually "origin")
	 * @param branch	branch to use (or NULL for the default)
	 *
	 * @return a map of module names to pull results, indicating what updates occurred
	 *
	 * @throws GitAPIException
	 * @throws IOException
	 * @throws ConfigInvalidException
	 */
	public Map<String, PullResult> pullComplete(String remote, String branch)
			throws GitAPIException, IOException, ConfigInvalidException {
		// Get the submodule list.
		Map<String, SubmoduleStatus> submoduleMap = this.repoGit.submoduleStatus().call();
		final int subTotal = submoduleMap.size();
		// We will need a result for the top module and one for each submodule.
		Map<String, PullResult> retVal = new TreeMap<String, PullResult>();
		// Pull the base module.
		PullCommand cmd = this.repoGit.pull();
		cmd.setRebase(false);
		cmd.setRemote(remote);
		cmd.setRemoteBranchName(branch);
		log.info("Pulling parent module of {}.", this.baseName);
		PullResult result = cmd.call();
		retVal.put("(parent)", result);
		// Now pull all the submodules. We need to do a submodule walk.
		if (subTotal > 0) {
			int subCount = 0;
			try (SubmoduleWalk walk = SubmoduleWalk.forIndex(this.localRepo)) {
				while (walk.next()) {
					try (Repository subRepo = walk.getRepository()) {
						if (subRepo != null) {
							subCount++;
							log.info("Pulling submodule {} of {}: {}.", subCount, subTotal,
									walk.getModuleName());
							try (Git subGit = new Git(subRepo)) {
								cmd = subGit.pull();
								cmd.setRemoteBranchName(branch);
								cmd.setRemote(remote);
								result = cmd.call();
								retVal.put(walk.getModuleName(), result);
							}
						}
					}
				}
			}
		}
		return retVal;
	}

	/**
	 * @return TRUE if this repo has submodules, else FALSE
	 *
	 * @throws GitAPIException
	 */
	public boolean hasSubmodules() throws GitAPIException {
		Map<String, SubmoduleStatus> submoduleMap = this.repoGit.submoduleStatus().call();
		return (submoduleMap.size() > 0);
	}

	/**
	 * @return a message about a pull result
	 *
	 * @param result	pull result to parse
	 */
	public static String resultMessageFor(PullResult result) {
		StringBuilder retVal = new StringBuilder(80);
		// Get the merge result and check status.
		MergeResult mergeInfo = result.getMergeResult();
		if (mergeInfo == null)
			retVal.append("NO_MERGE");
		else
			retVal.append(mergeInfo.getMergeStatus().toString());
		// Get the fetch result and count updates.
		FetchResult fetchInfo = result.getFetchResult();
		if (fetchInfo != null) {
			int updateCount = fetchInfo.getTrackingRefUpdates().size();
			if (updateCount > 0)
				retVal.append(", ").append(updateCount).append(" refs fetched");
			String msg = fetchInfo.getMessages();
			if (! StringUtils.isBlank(msg))
				retVal.append(", ").append(msg);
		}
		return retVal.toString();
	}

	@Override
	public String toString() {
		String branchName;
		try {
			branchName = this.localRepo.getBranch();
			if (branchName == null) branchName = "(detached)";
		} catch (IOException e) {
			branchName = e.toString();
		}
		return this.baseName + ": branch " + branchName;
	}

	/**
	 * @return the remote branch name for this repo (or NULL if none)
	 *
	 * @param remote	target remote (usually "origin")
	 */
	public String getBranch(String remote) {
		String retVal = null;
		try {
			// Form a search string from the remote name.
			String remoteIndicator = "/" + remote + "/";
			// Get the branches.
			ListBranchCommand cmd = this.repoGit.branchList();
			cmd.setListMode(ListMode.REMOTE);
			List<Ref> branches = cmd.call();
			Iterator<Ref> iter = branches.iterator();
			// Find the first ref for the target remote.
			while (retVal == null && iter.hasNext()) {
				Ref refI = iter.next();
				String refString = refI.getName();
				if (refString.contains(remoteIndicator)) {
					// Here we've found the desired remote. Get its leaf referenc/
					String leafName = refI.getLeaf().getName();
					// Shorten us to the branch part of the name.
					retVal = StringUtils.substringAfterLast(leafName, "/");
				}
			}
			if (retVal == null)
				log.error("Could not find remote {} in {}.", remote, this.baseName);
		} catch (GitAPIException e) {
			log.error("Error processing branch list for {}: {}", this.baseName, e.toString());
		}
		return retVal;
	}

	@Override
	public void close() {
		// Insure the git repo is closed.
		if (this.repoGit != null)
			this.repoGit.close();
		if (this.localRepo != null)
			this.localRepo.close();
	}

}
