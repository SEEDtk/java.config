/**
 *
 */
package org.theseed.config.git;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		this.localRepo = new FileRepository(gitLoc);
		this.repoGit = new Git(this.localRepo);
		this.baseName = loc.getName();
	}

	/**
	 * Pull the latest version of the top-level repo.
	 *
	 * @param remote	remote tag to use (usually "origin")
	 *
	 * @return a merge result, indicating what updates occurred
	 *
	 * @throws GitAPIException
	 */
	public PullResult pull(String remote) throws GitAPIException {
		PullCommand cmd = this.repoGit.pull();
		cmd.setRemote(remote);
		log.info("Pulling remote {} for module {}.", remote, this.baseName);
		PullResult retVal = cmd.call();
		return retVal;
	}

	/**
	 * Pull the latest version of the repo and all its submodules (if any).
	 *
	 * @param remote	remote tag to use (usually "origin")
	 * @param branch	branch to use for submodules (usually "master")
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
		cmd.setRemote(remote);
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

	@Override
	public void close() {
		// Insure the git repo is closed.
		this.repoGit.close();
	}

	/**
	 * @return a message about a pull result
	 *
	 * @param result	pull result to parse
	 */
	public static String resultMessageFor(PullResult result) {
		StringBuilder retVal = new StringBuilder(80);
		// Get the rebase result and check status.
		RebaseResult rebaseInfo = result.getRebaseResult();
		if (rebaseInfo == null)
			retVal.append("NO_REBASE");
		else
			retVal.append(rebaseInfo.getStatus().toString());
		// Get the fetch result and count updates.
		FetchResult fetchInfo = result.getFetchResult();
		if (fetchInfo != null) {
			int updateCount = fetchInfo.getTrackingRefUpdates().size();
			if (updateCount > 0)
				retVal.append(", ").append(updateCount).append(" commits fetched");
			String msg = fetchInfo.getMessages();
			if (! StringUtils.isBlank(msg))
				retVal.append(", ").append(msg);
		}
		return retVal.toString();
	}

}
