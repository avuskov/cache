package auskov;

import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class MyCacheEhcacheImplCustomizedPropsTest extends MyCacheCustomizedPropsTest {
    @Override
    protected MyCache createANewCache(Properties props) throws InvalidPropertiesFormatException {
        return MyCacheEhcacheImpl.createCash(props);
    }
}