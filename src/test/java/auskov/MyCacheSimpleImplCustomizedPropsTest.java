package auskov;

import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class MyCacheSimpleImplCustomizedPropsTest extends AbstractMyCacheCustomizedPropsTest {
    @Override
    protected MyCache createANewCache(Properties props) throws InvalidPropertiesFormatException {
        return MyCacheSimpleImpl.createCash(props);
    }

    //todo tests of cache.tiers.put.to
}