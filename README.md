# FsmStateManagementClient
FsmStateManagement's java based client to manage transactions

- To create finite state machine fsm client searchs for states.json under resource folder.
- If states.json can not be found, creation of fsm will fail.

```
[
  {
    "createTest": [
      {
        "stateName": "s1",
        "httpMethod": "GET",
        "revertEndpoint": "/api/v1/revert",
        "data": "${s1_revert_data}",
        "events": [
          {
            "eventName": "event1",
            "nextStateName": "s2"
          },
          {
            "eventName": "event3",
            "nextStateName": "s3"
          }
        ]
      },
      {
        "stateName": "s2",
        "httpMethod": "GET",
        "revertEndpoint": "/api/v1/revert",
        "data": "${s2_revert_data}",
        "events": [
          {
            "eventName": "event2",
            "nextStateName": "s1"
          }
        ]
      },
      {
        "stateName": "s3",
        "httpMethod": "GET",
        "revertEndpoint": "/api/v1/revert",
        "data": "${s3_revert_data}",
        "events": [
          {
            "eventName": "event4",
            "nextStateName": "s1"
          }
        ]
      }
    ]
  }
]

```

```
@FeignClient(name = "self", url = "${self.url}")
public interface ExampleFeign {

    @RequestMapping(method = RequestMethod.POST)
    @FsmTrace(fsmStatesName = "createTest", httpMethod = "PATCH", endpoint = "http://localhost:8088/api/{0}/{1}?test={2}",
            pathVariable = {"#0.id", "test"}, requestParams = {"#0.testName"})
    TestResource createTest(@RequestBody TestDto testDto);
}

public class MyRestController {

    @PostMapping
    @FsmTraceState(eventName = "event1")
    public ResponseEntity<TestResource> createTestModel(@RequestBody TestDto testDto) {
        TestModel createdTestModel = testService.createTestModel(conversionService.convert(testDto, TestModel.class));
        return ResponseEntity.ok(conversionService.convert(createdTestModel, TestResource.class));
    }

    @PatchMapping
    public ResponseEntity<TestResource> selfInvokedCreateTest(@RequestBody TestDto testDto) {
        return ResponseEntity.ok(selfFeign.createTest(testDto));
    }
}

```

FsmStateManagement Client is currently implemented for feign clients. Later implementation will be suitable for restTemplate as well.
There are two annotations that manage the rollback mechanism. @FsmTrace and @FsmTraceState have different behavior.

# @FsmTrace
 -  Creates Fsm and waits for response until all transaction is complete. If transaction fails then rollback mechanism is trigger by FsmStateManagement server. As you can see from above example, there are endpoint, method, requestParams, pathVariables and data fields.
These fields are necessary for server to notify microservices.

# @FsmTraceState
- Fsm moves the next state by given event automatically. Also if any exception occurs in next state, fsm fail endpoint is triggered automatically as well.


@FsmTrace annotation is used on feign client methods, @FsmTraceState annotation is used in Controller methods.


**There is fsm_transaction_id in the request header for each iteration of microservices. This transaction_id is responsible for managing transactions until all transaction is complete.


# Next Features
- RestTemplate implementation will be added.
- Rabbitmq implementation will be added.
- Conductor Netflix(Workflow management) will be added.

## SEE ALSO
https://github.com/grkn/FsmStateManagement



