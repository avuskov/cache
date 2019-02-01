package auskov;

import java.io.IOException;

public class MyCacheEhcacheImplTest extends AbstractMyCacheTest {
    @Override
    protected MyCache createANewCache() throws IOException {
        return MyCacheEhcacheImpl.createCash();
    }
}