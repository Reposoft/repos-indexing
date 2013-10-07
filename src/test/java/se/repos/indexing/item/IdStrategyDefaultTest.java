package se.repos.indexing.item;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Test;

import se.repos.indexing.IdStrategy;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdArg;

public class IdStrategyDefaultTest {

	@Test
	public void testRepository() {
		IdStrategy strategy = new IdStrategyDefault();
		CmsRepository repo = new CmsRepository("http://some.host:123/svn/repo1");
		RepoRevision rev = new RepoRevision(1, new Date());
		assertEquals("some.host:123/svn/repo1/a/b.txt@1", strategy.getId(repo, rev, new CmsItemPath("/a/b.txt")));
		assertNotEquals("repository id must be distinguished from root item id",
				strategy.getIdRepository(repo), strategy.getIdHead(repo, null));
	}
	
	@Test
	public void testSubclass() {
		IdStrategy strategy = new IdStrategyDefault() {

			@Override
			protected String getPrefix(CmsRepository repository) {
				return "x-svn://" + repository.getHost() + repository.getParentPath() + "/" + repository.getName() + "^";
			}

			@Override
			protected String getRootPath() {
				return "/"; // logical ID definition
			}

			@Override
			protected String getPegSeparator() {
				return "?p=";
			}
			
		};
		
		CmsItemId doc1 = new CmsItemIdArg("x-svn://my.host:1234/svn/demo1^/vvab/xml/documents/900108.xml").withPegRev(136L);
		assertEquals("x-svn://my.host:1234/svn/demo1^#", strategy.getIdRepository(doc1.getRepository()));
		assertEquals("x-svn://my.host:1234/svn/demo1^/vvab/xml/documents/900108.xml", strategy.getIdHead(doc1));
		assertEquals("x-svn://my.host:1234/svn/demo1^/vvab/xml/documents/900108.xml?p=136", strategy.getId(doc1));
	}

}
