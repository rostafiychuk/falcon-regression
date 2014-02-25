/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.regression.prism;


import org.apache.falcon.regression.core.bundle.Bundle;
import org.apache.falcon.regression.core.generated.dependencies.Frequency;
import org.apache.falcon.regression.core.generated.feed.Feed;
import org.apache.falcon.regression.core.generated.feed.Location;
import org.apache.falcon.regression.core.generated.feed.LocationType;
import org.apache.falcon.regression.core.helpers.ColoHelper;
import org.apache.falcon.regression.core.helpers.PrismHelper;
import org.apache.falcon.regression.core.supportClasses.Consumer;
import org.apache.falcon.regression.core.supportClasses.ENTITY_TYPE;
import org.apache.falcon.regression.core.util.HadoopUtil;
import org.apache.falcon.regression.core.util.Util;
import org.apache.falcon.regression.core.util.Util.URLS;
import org.apache.falcon.regression.testHelper.BaseTestClass;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.oozie.client.BundleJob;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.CoordinatorJob;
import org.apache.oozie.client.OozieClientException;
import org.apache.oozie.client.WorkflowJob;
import org.apache.oozie.client.XOozieClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.Assert;
import org.testng.TestNGException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.log4testng.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@Test(groups = "embedded")
public class RetentionTest extends BaseTestClass {
    static Logger logger = Logger.getLogger(RetentionTest.class);

    ColoHelper cluster1;
    private Bundle bundle;

    public RetentionTest(){
        super();
        cluster1 = servers.get(0);
    }

    @BeforeMethod(alwaysRun = true)
    public void testName(Method method) {
        logger.info("test name: " + method.getName());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        finalCheck(bundle);
    }

    @Test
    public void testRetentionWithEmptyDirectories() throws Exception {
        // test for https://issues.apache.org/jira/browse/FALCON-321
        final Bundle bundle = Util.getBundleData("RetentionBundles/valid/bundle1")[0];
        testRetention(bundle, "24", "hours", true, "daily", false);
    }

    @Test(groups = {"0.1", "0.2", "prism"}, dataProvider = "betterDP", priority = -1)
    public void testRetention(Bundle b, String period, String unit, boolean gaps, String dataType,
                              boolean withData) throws Exception {
        bundle = new Bundle(b, cluster1);
        displayDetails(period, unit, gaps, dataType);

        System.setProperty("java.security.krb5.realm", "");
        System.setProperty("java.security.krb5.kdc", "");
        String feed = setFeedPathValue(Util.getInputFeedFromBundle(bundle), getFeedPathValue(dataType));
        feed = insertRetentionValueInFeed(feed, unit + "(" + period + ")");
        bundle.getDataSets().remove(Util.getInputFeedFromBundle(bundle));
        bundle.getDataSets().add(feed);
        bundle.generateUniqueBundle();

        bundle.submitClusters(prism);

        if (Integer.parseInt(period) > 0) {
            Util.assertSucceeded(prism.getFeedHelper()
                    .submitEntity(URLS.SUBMIT_URL, Util.getInputFeedFromBundle(bundle)));

            replenishData(cluster1, dataType, gaps, withData);

            CommonDataRetentionWorkflow(cluster1, bundle, Integer.parseInt(period), unit);
        } else {
            Util.assertFailed(prism.getFeedHelper()
                    .submitEntity(URLS.SUBMIT_URL, Util.getInputFeedFromBundle(bundle)));
        }
    }

    private String setFeedPathValue(String feed, String pathValue) throws Exception {
        JAXBContext feedContext = JAXBContext.newInstance(Feed.class);
        Feed feedObject = (Feed) feedContext.createUnmarshaller().unmarshal(new StringReader(feed));

        //set the value
        for (Location location : feedObject.getLocations().getLocation()) {
            if (location.getType().equals(LocationType.DATA)) {
                location.setPath(pathValue);
            }
        }

        StringWriter feedWriter = new StringWriter();
        feedContext.createMarshaller().marshal(feedObject, feedWriter);
        return feedWriter.toString();
    }

    private void finalCheck(Bundle bundle) throws Exception {
        prism.getFeedHelper().delete(URLS.DELETE_URL, Util.getInputFeedFromBundle(bundle));
        verifyFeedDeletion(Util.getInputFeedFromBundle(bundle), cluster1);
    }

    private void displayDetails(String period, String unit, boolean gaps, String dataType)
    throws Exception {
        System.out.println("***********************************************");
        System.out.println("executing for:");
        System.out.println(unit + "(" + period + ")");
        System.out.println("gaps=" + gaps);
        System.out.println("dataType=" + dataType);
        System.out.println("***********************************************");
    }

    private void replenishData(ColoHelper helper, String dataType, boolean gap,
                               boolean withData) throws Exception {
        int skip = 0;

        if (gap) {
            Random r = new Random();
            skip = gaps[r.nextInt(gaps.length)];
        }

        if (dataType.equalsIgnoreCase("daily")) {
            replenishData(helper, convertDatesToFolders(getDailyDatesOnEitherSide
                    (36, skip), skip), withData);
        } else if (dataType.equalsIgnoreCase("yearly")) {
            replenishData(helper, getYearlyDatesOnEitherSide(10, skip), withData);
        } else if (dataType.equalsIgnoreCase("monthly")) {
            replenishData(helper, getMonthlyDatesOnEitherSide(30, skip), withData);
        }
    }

    private String getFeedPathValue(String dataType) throws Exception {
        if (dataType.equalsIgnoreCase("monthly")) {
            return "/retention/testFolders/${YEAR}/${MONTH}";
        }
        if (dataType.equalsIgnoreCase("daily")) {
            return "/retention/testFolders/${YEAR}/${MONTH}/${DAY}/${HOUR}";
        }
        if (dataType.equalsIgnoreCase("yearly")) {
            return "/retention/testFolders/${YEAR}";
        }
        return null;
    }

    private static void CommonDataRetentionWorkflow(ColoHelper helper, Bundle bundle, int time,
                                                    String interval)
    throws JAXBException, OozieClientException, IOException, URISyntaxException,
    InterruptedException {
        //get Data created in the cluster
        List<String> initialData = getHadoopData(helper, Util.getInputFeedFromBundle(bundle));

        helper.getFeedHelper()
                .schedule(URLS.SCHEDULE_URL, Util.getInputFeedFromBundle(bundle));
        logger.info(helper.getClusterHelper().getActiveMQ());
        logger.info(Util.readDatasetName(Util.getInputFeedFromBundle(bundle)));
        Consumer consumer =
                new Consumer("FALCON." + Util.readDatasetName(Util.getInputFeedFromBundle(bundle)),
                        helper.getClusterHelper().getActiveMQ());
        consumer.start();

        DateTime currentTime = new DateTime(DateTimeZone.UTC);
        String bundleId = Util.getBundles(helper.getFeedHelper().getOozieClient(),
                Util.readDatasetName(Util.getInputFeedFromBundle(bundle)), ENTITY_TYPE.FEED).get(0);

        List<String> workflows = getFeedRetentionJobs(helper, bundleId);
        logger.info("got a workflow list of length:" + workflows.size());
        Collections.sort(workflows);

        for (String workflow : workflows) {
            logger.info(workflow);
        }

        if (!workflows.isEmpty()) {
            String workflowId = workflows.get(0);
            String status = getWorkflowInfo(helper, workflowId);
            while (!(status.equalsIgnoreCase("KILLED") || status.equalsIgnoreCase("FAILED") ||
                    status.equalsIgnoreCase("SUCCEEDED"))) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.error(e.getMessage());
                }
                status = getWorkflowInfo(helper, workflowId);
            }
        }

        consumer.stop();

        logger.info("deleted data which has been received from messaging queue:");
        for (HashMap<String, String> data : consumer.getMessageData()) {
            logger.info("*************************************");
            for (String key : data.keySet()) {
                logger.info(key + "=" + data.get(key));
            }
            logger.info("*************************************");
        }

        //now look for cluster data
        List<String> finalData =
                getHadoopData(helper, Util.getInputFeedFromBundle(bundle));

        //now see if retention value was matched to as expected
        List<String> expectedOutput =
                filterDataOnRetention(Util.getInputFeedFromBundle(bundle), time, interval,
                        currentTime, initialData);

        logger.info("initial data in system was:");
        for (String line : initialData) {
            logger.info(line);
        }

        logger.info("system output is:");
        for (String line : finalData) {
            logger.info(line);
        }

        logger.info("actual output is:");
        for (String line : expectedOutput) {
            logger.info(line);
        }

        validateDataFromFeedQueue(helper,
                Util.readDatasetName(Util.getInputFeedFromBundle(bundle)),
                consumer.getMessageData(), expectedOutput, initialData);

        Assert.assertEquals(finalData.size(), expectedOutput.size(),
                "sizes of outputs are different! please check");

        Assert.assertTrue(Arrays.deepEquals(finalData.toArray(new String[finalData.size()]),
                expectedOutput.toArray(new String[expectedOutput.size()])));
    }

    private static void replenishData(ColoHelper helper, List<String> folderList, boolean uploadData)
    throws IOException, InterruptedException {
        //purge data first
        FileSystem fs = HadoopUtil.getFileSystem(helper.getFeedHelper().getHadoopURL());
        HadoopUtil.deleteDirIfExists("/retention/testFolders/", fs);

        createHDFSFolders(helper, folderList, uploadData);
    }

    private static void createHDFSFolders(PrismHelper prismHelper, List<String> folderList,
                                          boolean uploadData)
    throws IOException, InterruptedException {
        final FileSystem fs = prismHelper.getClusterHelper().getHadoopFS();

        folderList.add("somethingRandom");

        for (final String folder : folderList) {
            final String pathString = "/retention/testFolders/" + folder;
            logger.info(pathString);
            fs.mkdirs(new Path(pathString));
            if(uploadData) {
                fs.copyFromLocalFile(new Path("log_01.txt"), new Path(pathString));
            }
        }
    }

    private static void validateDataFromFeedQueue(PrismHelper prismHelper, String feedName,
                                                  List<HashMap<String, String>> queueData,
                                                  List<String> expectedOutput,
                                                  List<String> input) throws OozieClientException {

        //just verify that each element in queue is same as deleted data!
        input.removeAll(expectedOutput);

        List<String> jobIds = Util.getCoordinatorJobs(prismHelper,
                Util.getBundles(prismHelper.getFeedHelper().getOozieClient(),
                        feedName, ENTITY_TYPE.FEED).get(0)
        );

        //create queuedata folderList:
        List<String> deletedFolders = new ArrayList<String>();

        for (HashMap<String, String> data : queueData) {
            if (data != null) {
                Assert.assertEquals(data.get("entityName"), feedName);
                String[] splitData = data.get("feedInstancePaths").split("testFolders/");
                deletedFolders.add(splitData[splitData.length - 1]);
                Assert.assertEquals(data.get("operation"), "DELETE");
                Assert.assertEquals(data.get("workflowId"), jobIds.get(0));

                //verify other data also
                Assert.assertEquals(data.get("topicName"), "FALCON." + feedName);
                Assert.assertEquals(data.get("brokerImplClass"),
                        "org.apache.activemq.ActiveMQConnectionFactory");
                Assert.assertEquals(data.get("status"), "SUCCEEDED");
                Assert.assertEquals(data.get("brokerUrl"),
                        prismHelper.getFeedHelper().getActiveMQ());

            }
        }

        //now make sure queueData and input lists are same
        Assert.assertEquals(deletedFolders.size(), input.size(),
                "Output size is different than expected!");
        Assert.assertTrue(Arrays.deepEquals(input.toArray(new String[input.size()]),
                        deletedFolders.toArray(new String[deletedFolders.size()])),
                "It appears that the data that is received from queue and the data deleted are " +
                        "not same!");
    }

    private static List<String> getHadoopData(ColoHelper helper, String feed)
    throws JAXBException, IOException {
        return Util.getHadoopDataFromDir(helper, feed, "/retention/testFolders/");
    }

    private static String insertRetentionValueInFeed(String feed, String retentionValue)
    throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Feed.class);
        Unmarshaller um = context.createUnmarshaller();
        Feed feedObject = (Feed) um.unmarshal(new StringReader(feed));

        //insert retentionclause
        feedObject.getClusters().getCluster().get(0).getRetention()
                .setLimit(new Frequency(retentionValue));

        for (org.apache.falcon.regression.core.generated.feed.Cluster cluster : feedObject
                .getClusters().getCluster()) {
            cluster.getRetention().setLimit(new Frequency(retentionValue));
        }

        StringWriter writer = new StringWriter();
        Marshaller m = context.createMarshaller();
        m.marshal(feedObject, writer);

        return writer.toString();

    }

    private static void verifyFeedDeletion(String feed, ColoHelper... helpers)
    throws JAXBException, IOException {
        for (ColoHelper helper : helpers) {
            String directory = "/projects/ivory/staging/" + helper.getFeedHelper().getServiceUser()
                    + "/workflows/feed/" + Util.readDatasetName(feed);
            final FileSystem fs = helper.getProcessHelper().getHadoopFS();
            //make sure feed bundle is not there
            Assert.assertFalse(fs.isDirectory(new Path(directory)),
                    "Feed " + Util.readDatasetName(feed) + " did not have its bundle removed!!!!");
        }

    }

    private static List<String> convertDatesToFolders(List<String> dateList, int skipInterval) {
        logger.info("converting dates to folders....");
        List<String> folderList = new ArrayList<String>();

        for (String date : dateList) {
            for (int i = 0; i < 24; i += skipInterval + 1) {
                if (i < 10) {
                    folderList.add(date + "/0" + i);
                } else {
                    folderList.add(date + "/" + i);
                }
            }
        }

        return folderList;
    }

    private static List<String> getDailyDatesOnEitherSide(int interval, int skip) {

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd");

        DateTime today = new DateTime(DateTimeZone.UTC);
        logger.info("today is: " + today.toString());

        List<String> dates = new ArrayList<String>();
        dates.add(formatter.print(today));

        //first lets get all dates before today
        for (int backward = 1; backward <= interval; backward += skip + 1) {
            dates.add(formatter.print(today.minusDays(backward)));
        }

        //now the forward dates
        for (int i = 1; i <= interval; i += skip + 1) {
            dates.add(formatter.print(today.plusDays(i)));
        }

        return dates;
    }

    private static List<String> getYearlyDatesOnEitherSide(int interval, int skip) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy");
        DateTime today = new DateTime(DateTimeZone.UTC);
        logger.info("today is: " + today.toString());

        List<String> dates = new ArrayList<String>();
        dates.add(formatter.print(new LocalDate(today)));

        //first lets get all dates before today
        for (int backward = 1; backward <= interval; backward += skip + 1) {
            dates.add(formatter.print(new LocalDate(today.minusYears(backward))));
        }

        //now the forward dates
        for (int i = 1; i <= interval; i += skip + 1) {
            dates.add(formatter.print(new LocalDate(today.plusYears(i))));
        }

        return dates;
    }

    private static List<String> getMonthlyDatesOnEitherSide(int interval, int skip) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM");
        DateTime today = new DateTime(DateTimeZone.UTC);
        logger.info("today is: " + today.toString());

        List<String> dates = new ArrayList<String>();
        dates.add(formatter.print((today)));

        //first lets get all dates before today
        for (int backward = 1; backward <= interval; backward += skip + 1) {
            dates.add(formatter.print(new LocalDate(today.minusMonths(backward))));
        }

        //now the forward dates
        for (int i = 1; i <= interval; i += skip + 1) {
            dates.add(formatter.print(new LocalDate(today.plusMonths(i))));
        }

        return dates;
    }

    private static List<String> getFeedRetentionJobs(PrismHelper prismHelper, String bundleID)
    throws OozieClientException, InterruptedException {
        List<String> jobIds = new ArrayList<String>();
        XOozieClient oozieClient = prismHelper.getFeedHelper().getOozieClient();
        BundleJob bundleJob = oozieClient.getBundleJobInfo(bundleID);
        CoordinatorJob jobInfo =
                oozieClient.getCoordJobInfo(bundleJob.getCoordinators().get(0).getId());

        while (jobInfo.getActions().isEmpty()) {
            //keep dancing
            jobInfo = oozieClient.getCoordJobInfo(bundleJob.getCoordinators().get(0).getId());
        }

        logger.info("got coordinator jobInfo array of length:" + jobInfo.getActions());
        for (CoordinatorAction action : jobInfo.getActions()) {
            logger.info(action.getId());
        }
        for (CoordinatorAction action : jobInfo.getActions()) {
            CoordinatorAction actionInfo = oozieClient.getCoordActionInfo(action.getId());

            for(int i=0; i < 180; ++i) {
                actionInfo = oozieClient.getCoordActionInfo(action.getId());
                if(actionInfo.getStatus() == CoordinatorAction.Status.SUCCEEDED ||
                        actionInfo.getStatus() == CoordinatorAction.Status.KILLED ||
                        actionInfo.getStatus() == CoordinatorAction.Status.FAILED ) {
                    break;
                }
                Thread.sleep(10000);
            }
            Assert.assertEquals(actionInfo.getStatus(),CoordinatorAction.Status.SUCCEEDED,
                    "Action did not succeed.");
            jobIds.add(action.getId());

        }


        return jobIds;

    }

    private static String getWorkflowInfo(PrismHelper prismHelper, String workflowId)
    throws OozieClientException {
        XOozieClient oozieClient = prismHelper.getClusterHelper().getOozieClient();
        logger.info("fetching info for workflow with id: " + workflowId);
        WorkflowJob job = oozieClient.getJobInfo(workflowId);
        return job.getStatus().toString();
    }

    private static List<String> filterDataOnRetention(String feed, int time, String interval,
                                                     DateTime endDate,
                                                     List<String> inputData) throws JAXBException {
        String locationType = "";
        String appender = "";

        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm");
        List<String> finalData = new ArrayList<String>();

        //determine what kind of data is there in the feed!
        JAXBContext feedContext = JAXBContext.newInstance(Feed.class);
        Feed feedObject = (Feed) feedContext.createUnmarshaller().unmarshal(new StringReader(feed));

        for (Location location : feedObject
                .getLocations()
                .getLocation()) {
            if (location.getType().equals(LocationType.DATA)) {
                locationType = location.getPath();
            }
        }


        if (locationType.equalsIgnoreCase("") || locationType.equalsIgnoreCase(null)) {
            throw new TestNGException("location type was not mentioned in your feed!");
        }

        if (locationType.equalsIgnoreCase("/retention/testFolders/${YEAR}/${MONTH}")) {
            appender = "/01/00/01";
        } else if (locationType
                .equalsIgnoreCase("/retention/testFolders/${YEAR}/${MONTH}/${DAY}")) {
            appender = "/01"; //because we already take care of that!
        } else if (locationType
                .equalsIgnoreCase("/retention/testFolders/${YEAR}/${MONTH}/${DAY}/${HOUR}")) {
            appender = "/01";
        } else if (locationType.equalsIgnoreCase("/retention/testFolders/${YEAR}")) {
            appender = "/01/01/00/01";
        }

        //convert the start and end date boundaries to the same format


        //end date is today's date
        formatter.print(endDate);
        String startLimit = "";

        if (interval.equalsIgnoreCase("minutes")) {
            startLimit =
                    formatter.print(new DateTime(endDate, DateTimeZone.UTC).minusMinutes(time));
        } else if (interval.equalsIgnoreCase("hours")) {
            startLimit = formatter.print(new DateTime(endDate, DateTimeZone.UTC).minusHours(time));
        } else if (interval.equalsIgnoreCase("days")) {
            startLimit = formatter.print(new DateTime(endDate, DateTimeZone.UTC).minusDays(time));
        } else if (interval.equalsIgnoreCase("months")) {
            startLimit =
                    formatter.print(new DateTime(endDate, DateTimeZone.UTC).minusDays(31 * time));

        }


        //now to actually check!
        for (String testDate : inputData) {
            if (!testDate.equalsIgnoreCase("somethingRandom")) {
                if ((testDate + appender).compareTo(startLimit) >= 0) {
                    finalData.add(testDate);
                }
            } else {
                finalData.add(testDate);
            }
        }

        return finalData;

    }

    final static int[] gaps = new int[]{2, 4, 5, 1};

    @DataProvider(name = "betterDP")
    public Object[][] getTestData(Method m) throws Exception {
        Bundle[] bundles = Util.getBundleData("RetentionBundles/valid/bundle1");
        String[] periods = new String[]{"0", "10080", "60", "8", "24"}; // a negative value like -4 should be covered in validation scenarios.
        String[] units = new String[]{"hours", "days"};// "minutes","hours","days",
        boolean[] gaps = new boolean[]{false, true};
        String[] dataTypes = new String[]{"daily", "yearly", "monthly"};
        Object[][] testData = new Object[bundles.length * units.length * periods.length * gaps
                .length * dataTypes.length][6];

        int i = 0;

        for (Bundle bundle : bundles) {
            for (String unit : units) {
                for (String period : periods) {
                    for (boolean gap : gaps) {
                        for (String dataType : dataTypes) {
                            testData[i][0] = bundle;
                            testData[i][1] = period;
                            testData[i][2] = unit;
                            testData[i][3] = gap;
                            testData[i][4] = dataType;
                            testData[i][5] = true;
                            i++;
                        }
                    }
                }
            }
        }

        return testData;
    }

}
