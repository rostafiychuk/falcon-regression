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

package org.apache.falcon.regression;

import org.apache.falcon.regression.core.bundle.Bundle;
import org.apache.falcon.entity.v0.EntityType;
import org.apache.falcon.entity.v0.Frequency.TimeUnit;
import org.apache.falcon.regression.core.helpers.ColoHelper;
import org.apache.falcon.regression.core.supportClasses.Consumer;
import org.apache.falcon.regression.core.util.BundleUtil;
import org.apache.falcon.regression.core.util.HadoopUtil;
import org.apache.falcon.regression.core.util.InstanceUtil;
import org.apache.falcon.regression.core.util.OSUtil;
import org.apache.falcon.regression.core.util.TimeUtil;
import org.apache.falcon.regression.core.util.Util;
import org.apache.falcon.regression.testHelper.BaseTestClass;
import org.apache.hadoop.fs.FileSystem;
import org.apache.log4j.Logger;
import org.apache.oozie.client.CoordinatorAction;
import org.apache.oozie.client.OozieClient;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;


/**
 * Null output process tests.
 */
@Test(groups = "embedded")
public class NoOutputProcessTest extends BaseTestClass {

    ColoHelper cluster = servers.get(0);
    FileSystem clusterFS = serverFS.get(0);
    OozieClient clusterOC = serverOC.get(0);
    String testDir = baseHDFSDir + "/NoOutputProcessTest";
    String inputPath = testDir + "/input" + dateTemplate;
    String aggregateWorkflowDir = testDir + "/aggregator";
    private static final Logger logger = Logger.getLogger(NoOutputProcessTest.class);

    @BeforeClass(alwaysRun = true)
    public void createTestData() throws Exception {

        logger.info("in @BeforeClass");
        HadoopUtil.uploadDir(clusterFS, aggregateWorkflowDir, OSUtil.RESOURCES_OOZIE);

        Bundle b = BundleUtil.readELBundle();
        b.generateUniqueBundle();
        b = new Bundle(b, cluster);

        String startDate = "2010-01-03T00:00Z";
        String endDate = "2010-01-03T03:00Z";

        b.setInputFeedDataPath(inputPath);
        String prefix = b.getFeedDataPathPrefix();
        HadoopUtil.deleteDirIfExists(prefix.substring(1), clusterFS);

        List<String> dataDates = TimeUtil.getMinuteDatesOnEitherSide(startDate, endDate, 20);
        HadoopUtil.flattenAndPutDataInFolder(clusterFS, OSUtil.NORMAL_INPUT, prefix, dataDates);
    }


    @BeforeMethod(alwaysRun = true)
    public void testName(Method method) throws Exception {
        logger.info("test name: " + method.getName());
        bundles[0] = BundleUtil.readELBundle();
        bundles[0].generateUniqueBundle();
        bundles[0] = new Bundle(bundles[0], cluster);
        bundles[0].setProcessWorkflow(aggregateWorkflowDir);
        bundles[0].setInputFeedDataPath(inputPath);
        bundles[0].setProcessValidity("2010-01-03T02:30Z", "2010-01-03T02:45Z");
        bundles[0].setProcessPeriodicity(5, TimeUnit.minutes);
        bundles[0].submitFeedsScheduleProcess(prism);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        removeBundles();
    }

    @Test(enabled = true, groups = {"singleCluster"})
    public void checkForJMSMsgWhenNoOutput() throws Exception {
        logger.info("attaching consumer to:   " + "FALCON.ENTITY.TOPIC");
        Consumer consumer =
            new Consumer("FALCON.ENTITY.TOPIC", cluster.getClusterHelper().getActiveMQ());
        consumer.start();

        //wait for all the instances to complete
        InstanceUtil.waitTillInstanceReachState(clusterOC, bundles[0].getProcessName(), 3,
            CoordinatorAction.Status.SUCCEEDED, EntityType.PROCESS);

        Assert.assertEquals(consumer.getMessageData().size(), 3,
            " Message for all the 3 instance not found");

        consumer.interrupt();

        Util.dumpConsumerData(consumer);
    }


    @Test(enabled = true, groups = {"singleCluster"})
    public void rm() throws Exception {
        Consumer consumerInternalMsg =
            new Consumer("FALCON.ENTITY.TOPIC", cluster.getClusterHelper().getActiveMQ());
        Consumer consumerProcess =
            new Consumer("FALCON." + bundles[0].getProcessName(),
                cluster.getClusterHelper().getActiveMQ());

        consumerInternalMsg.start();
        consumerProcess.start();

        //wait for all the instances to complete
        InstanceUtil.waitTillInstanceReachState(clusterOC, bundles[0].getProcessName(), 3,
            CoordinatorAction.Status.SUCCEEDED, EntityType.PROCESS);

        Assert.assertEquals(consumerInternalMsg.getMessageData().size(), 3,
            " Message for all the 3 instance not found");
        Assert.assertEquals(consumerProcess.getMessageData().size(), 3,
            " Message for all the 3 instance not found");

        consumerInternalMsg.interrupt();
        consumerProcess.interrupt();

        Util.dumpConsumerData(consumerInternalMsg);
        Util.dumpConsumerData(consumerProcess);
    }

}
