package org.jbake.app;

import ch.qos.logback.classic.spi.LoggingEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.jbake.TestUtils;
import org.jbake.app.configuration.ConfigUtil;
import org.jbake.app.configuration.DefaultJBakeConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AssetTest extends LoggingTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private DefaultJBakeConfiguration config;
    private File fixtureDir;

    @Before
    public void setup() throws Exception {
        fixtureDir = new File(this.getClass().getResource("/fixture").getFile());
        config = (DefaultJBakeConfiguration) new ConfigUtil().loadConfig(fixtureDir);
        config.setDestinationFolder(folder.getRoot());
        Assert.assertEquals(".html", config.getOutputExtension());
    }

    @Test
    public void testCopy() throws Exception {
        Asset asset = new Asset(config);
        asset.copy();

        File cssFile = new File(folder.getRoot().getPath() + File.separatorChar + "css" + File.separatorChar + "bootstrap.min.css");
        Assert.assertTrue("File " + cssFile.getAbsolutePath() + " does not exist", cssFile.exists());
        File imgFile = new File(folder.getRoot().getPath() + File.separatorChar + "img" + File.separatorChar + "glyphicons-halflings.png");
        Assert.assertTrue("File " + imgFile.getAbsolutePath() + " does not exist", imgFile.exists());
        File jsFile = new File(folder.getRoot().getPath() + File.separatorChar + "js" + File.separatorChar + "bootstrap.min.js");
        Assert.assertTrue("File " + jsFile.getAbsolutePath() + " does not exist", jsFile.exists());

        Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
    }

    @Test
    public void testCopySingleFile() throws Exception {
        Asset asset = new Asset(config);
        String cssSubPath = File.separatorChar + "css" + File.separatorChar + "bootstrap.min.css";
        String contentImgPath = File.separatorChar + "blog" + File.separatorChar + "2013" + File.separatorChar
            + "images" + File.separatorChar + "custom-image.jpg";

        // Copy single Asset File
        File expected = new File(folder.getRoot().getPath() + cssSubPath);
        Assert.assertFalse("cssFile should not exist before running the test; avoids false positives", expected.exists());
        File cssFile = new File(fixtureDir.getPath() + File.separatorChar + "assets" + cssSubPath);
        asset.copySingleFile(cssFile);
        Assert.assertTrue("Css asset file did not copy", expected.exists());

        // Copy single Content file
        expected = new File(folder.getRoot().getPath() + contentImgPath);
        Assert.assertFalse("content image file should not exist before running the test", expected.exists());
        File imgFile = new File(fixtureDir.getPath() + File.separatorChar + "content" + contentImgPath);
        asset.copySingleFile(imgFile);
        Assert.assertTrue("Content img file did not copy", expected.exists());
    }

    @Test
    public void shouldSkipCopyingSingleFileIfDirectory() throws IOException {

        Asset asset = new Asset(config);

        File emptyDir = folder.newFolder("emptyDir");
        File expectedDir = new File(fixtureDir.getCanonicalPath(), "emptyDir");

        asset.copySingleFile(emptyDir);

        Assert.assertFalse("Directory should be skipped", expectedDir.exists());
    }

    @Test
    public void shouldLogSkipCopyingSingleFileIfDirectory() throws IOException {

        Asset asset = new Asset(config);
        File emptyDir = folder.newFolder("emptyDir");

        asset.copySingleFile(emptyDir);

        verify(mockAppender, times(1)).doAppend(captorLoggingEvent.capture());

        LoggingEvent loggingEvent = captorLoggingEvent.getValue();
        assertThat(loggingEvent.getMessage()).isEqualTo("Skip copying single asset file [{}]. Is a directory.");

    }



    @Test
    public void testCopyCustomFolder() throws Exception {
        config.setAssetFolder(new File(config.getSourceFolder(),"/media"));
        Asset asset = new Asset(config);
        asset.copy();

        File favFile = new File(folder.getRoot().getPath() + File.separatorChar + "favicon.ico");
        Assert.assertTrue("File " + favFile.getAbsolutePath() + " does not exist", favFile.exists());

        Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
    }

    @Test
    public void testCopyIgnore() throws Exception {
        File assetFolder = folder.newFolder("ignoredAssets");
        FileUtils.copyDirectory(new File(this.getClass().getResource("/fixture/ignorables").getFile()), assetFolder);
        config.setAssetFolder(assetFolder);
        config.setAssetIgnoreHidden(true);
        TestUtils.hideAssets(assetFolder);
        Asset asset = new Asset(config);
        asset.copy(assetFolder);

        File testFile = new File(folder.getRoot(), "test.txt");
        Assert.assertTrue("File " + testFile.getAbsolutePath() + " does not exist", testFile.exists());
        File testIgnoreFile = new File(folder.getRoot(), ".test.txt");
        Assert.assertFalse("File " + testIgnoreFile.getAbsolutePath() + " does exist", testIgnoreFile.exists());

        Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
    }


    /**
     * Primary intention is to extend test cases to increase coverage.
     *
     * @throws Exception
     */
    @Test
    public void testWriteProtected() throws Exception {
        File assets = new File(config.getSourceFolder(), "assets");
        final File cssFile = new File(folder.newFolder("css"), "bootstrap.min.css");
        FileUtils.touch(cssFile);
        cssFile.setReadOnly();

        config.setAssetFolder(assets);
        config.setDestinationFolder(folder.getRoot());
        Asset asset = new Asset(config);
        asset.copy();

        Assert.assertFalse("At least one error during copy expected", asset.getErrors().isEmpty());
    }

    /**
     * Primary intention is to extend test cases to increase coverage.
     *
     * @throws Exception
     */
    @Test
    public void testUnlistable() throws Exception {
        config.setAssetFolder(new File(config.getSourceFolder(), "non-exsitent"));
        Asset asset = new Asset(config);
        asset.copy();
    }

    @Test
    public void testJBakeIgnoredFolder() {
        URL assetsUrl = this.getClass().getResource("/fixture/assets");
        File assets = new File(assetsUrl.getFile());
        Asset asset = new Asset(config);
        asset.copy(assets);

        File cssFile = new File(folder.getRoot().getPath() + File.separatorChar + "css" + File.separatorChar + "bootstrap.min.css");
        Assert.assertTrue("File " + cssFile.getAbsolutePath() + " does not exist", cssFile.exists());
        File imgFile = new File(folder.getRoot().getPath() + File.separatorChar + "img" + File.separatorChar + "glyphicons-halflings.png");
        Assert.assertTrue("File " + imgFile.getAbsolutePath() + " does not exist", imgFile.exists());
        File jsFile = new File(folder.getRoot().getPath() + File.separatorChar + "js" + File.separatorChar + "bootstrap.min.js");
        Assert.assertTrue("File " + jsFile.getAbsolutePath() + " does not exist", jsFile.exists());
        File ignorableFolder = new File(folder.getRoot().getPath() + File.separatorChar + "ignorablefolder");
        Assert.assertFalse("Folder " + ignorableFolder.getAbsolutePath() + " must not exist", ignorableFolder.exists());

        Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
    }


    @Test
    public void testCopyAssetsFromContent() {
        URL contentUrl = this.getClass().getResource("/fixture/content");
        File contents = new File(contentUrl.getFile());
        Asset asset = new Asset(config);
        asset.copyAssetsFromContent(contents);

        int totalFiles = countFiles(folder.getRoot());
        int expected = 3;

        Assert.assertTrue(String.format("Number of files copied must be %d but are %d", expected, totalFiles), totalFiles == expected);

        File pngFile = new File(folder.getRoot().getPath() + File.separatorChar + "blog" + File.separatorChar + "2012/images/custom-image.png");
        Assert.assertTrue("File " + pngFile.getAbsolutePath() + " does not exist", pngFile.exists());

        File jpgFile = new File(folder.getRoot().getPath() + File.separatorChar + "blog" + File.separatorChar + "2013/images/custom-image.jpg");
        Assert.assertTrue("File " + jpgFile.getAbsolutePath() + " does not exist", jpgFile.exists());

        File jsonFile = new File(folder.getRoot().getPath() + File.separatorChar + "blog" + File.separatorChar + "2012/sample.json");
        Assert.assertTrue("File " + jsonFile.getAbsolutePath() + " does not exist", jsonFile.exists());

        Assert.assertTrue("Errors during asset copying", asset.getErrors().isEmpty());
    }

    @Test
    public void testIsFileAsset() {
        File cssAsset = new File(config.getAssetFolder().getAbsolutePath() + File.separatorChar + "css" + File.separatorChar + "bootstrap.min.css");
        Assert.assertTrue(cssAsset.exists());
        File contentFile = new File(config.getContentFolder().getAbsolutePath() + File.separatorChar + "about.html");
        Assert.assertTrue(contentFile.exists());
        Asset asset = new Asset(config);

        Assert.assertTrue(asset.isAssetFile(cssAsset));
        Assert.assertFalse(asset.isAssetFile(contentFile));
    }

    private Integer countFiles(File path){
        int total = 0;
        FileFilter filesOnly = FileFilterUtils.fileFileFilter();
        FileFilter dirsOnly = FileFilterUtils.directoryFileFilter();
        File[] files = path.listFiles(filesOnly);
        System.out.println(files);
        total += files.length;
        for (File file : path.listFiles(dirsOnly)) {
            total += countFiles(file);
        }
        return total;
    }
}
