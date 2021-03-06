import React from "react";
import {mount} from "enzyme";
import DetailHeader from "./detail-header";
import jsonDocPayload from "../../assets/mock-data/explore/json-document-payload";

describe("Detail component", () => {
  let wrapper;

  describe("Using JSON document payload with primaryKey", () => {
    beforeEach(() => {
      wrapper = mount(
        <DetailHeader
          document={jsonDocPayload.data}
          uri="/Users/ban/Documents/Projects/dhf-files/store-data/products/games/ebb9671e-4c3d-4b33-810f-d57d7c5d5897.json"
          primaryKey="1000201"
          contentType="json"
          sources={jsonDocPayload.data.envelope.headers.sources}
        />);
    });

    test("component renders", () => {
      expect(true).toBe(true);
      expect(wrapper.exists("#header")).toBe(true);
      expect(wrapper.exists("#title")).toBe(true);
      expect(wrapper.exists("#summary")).toBe(true);
    });

    test("data renders", () => {
      expect(wrapper.exists("[data-cy=\"document-title\"]")).toBe(true);
      expect(wrapper.exists("[data-cy=\"document-timestamp\"]")).toBe(true);
      expect(wrapper.exists("[data-cy=\"document-source\"]")).toBe(true);
      expect(wrapper.exists("[data-cy=\"document-recordtype\"]")).toBe(true);
    });
    test("primaryKey renders", () => {
      expect(wrapper.exists("[data-cy=\"document-id\"]")).toBe(true);
    });
  });

  describe("Using JSON document payload without primaryKey", () => {
    beforeEach(() => {
      wrapper = mount(
        <DetailHeader
          document={jsonDocPayload.data}
          uri="/Users/ban/Documents/Projects/dhf-files/store-data/products/games/ebb9671e-4c3d-4b33-810f-d57d7c5d5897.json"
          primaryKey=""
          contentType="json"
          sources={jsonDocPayload.data.envelope.headers.sources}
        />);
    });

    test("component renders", () => {
      expect(true).toBe(true);
      expect(wrapper.exists("#header")).toBe(true);
      expect(wrapper.exists("#title")).toBe(true);
      expect(wrapper.exists("#summary")).toBe(true);
    });

    test("data renders", () => {
      expect(wrapper.exists("[data-cy=\"document-title\"]")).toBe(true);
      expect(wrapper.exists("[data-cy=\"document-timestamp\"]")).toBe(true);
      expect(wrapper.exists("[data-cy=\"document-source\"]")).toBe(true);
      expect(wrapper.exists("[data-cy=\"document-recordtype\"]")).toBe(true);
    });
    test("uri renders", () => {
      expect(wrapper.exists("[data-cy=\"document-uri\"]")).toBe(true);
    });
  });
  // TODO add test case for XML data
});
