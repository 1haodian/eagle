/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eagle.service.security.hdfs.test;

import eagle.service.security.hdfs.resolver.HDFSResourceResolver;
import eagle.service.alert.resolver.AttributeResolveException;
import eagle.service.alert.resolver.GenericAttributeResolveRequest;
import org.junit.Test;

public class HDFSResourceResolverTest {
	//@Test
	public void testHDFSResourceResolver() throws AttributeResolveException {
		GenericAttributeResolveRequest request = new GenericAttributeResolveRequest("/user","cluster1-dc1");
		HDFSResourceResolver resolve = new HDFSResourceResolver();
		System.out.println(resolve.resolve(request));
	}

	@Test
	public void test() {

	}
}