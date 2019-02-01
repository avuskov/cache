package auskov;

import java.io.IOException;

public class MyCacheEhcacheImplTest extends AbstractMyCacheTest {
    @Override
    MyCache createANewCache() throws IOException {
        return MyCacheEhcacheImpl.createCash();
    }
}